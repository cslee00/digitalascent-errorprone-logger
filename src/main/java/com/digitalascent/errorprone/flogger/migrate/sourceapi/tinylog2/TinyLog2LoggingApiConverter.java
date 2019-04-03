package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MatchResult;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments.matchAtIndex;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggingMethod;
import static java.util.Objects.requireNonNull;

/**
 * Tiny Log 2 API: https://tinylog.org/v2/javadoc/
 */
public final class TinyLog2LoggingApiConverter extends AbstractLoggingApiConverter {

    private final TinyLog2LogMessageHandler logMessageHandler = new TinyLog2LogMessageHandler();

    public TinyLog2LoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        super(floggerSuggestedFixGenerator, targetLogLevelFunction);
    }

    @Override
    protected boolean matchLoggingEnabledMethod(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return false;
    }

    @Override
    protected boolean matchLoggingMethod(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return loggingMethod().matches(methodInvocationTree, state);
    }

    @Override
    protected SuggestedFix migrateLoggingEnabledMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        throw new UnsupportedOperationException("TinyLog2 doesn't have logging enabled methods");
    }

    @Override
    protected boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return false;
    }

    @Override
    public boolean isLoggerVariable(VariableTree tree, VisitorState state) {
        return false;
    }

    @Override
    protected boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    @Override
    protected ImmutableFloggerLogContext migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                              VisitorState state, MigrationContext migrationContext) {
        ImmutableFloggerLogContext.Builder builder = ImmutableFloggerLogContext.builder();

        TargetLogLevel targetLogLevel;
        targetLogLevel = mapLogLevel(methodName);
        builder.targetLogLevel(targetLogLevel);

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();
        ExpressionTree throwableArgument = findThrowableArgument(remainingArguments,state);
        if( throwableArgument != null ) {
            builder.thrown(throwableArgument);
            remainingArguments = Arguments.removeFirst( remainingArguments );
        }

        ExpressionTree messageFormatArgument = remainingArguments.isEmpty() ? throwableArgument : remainingArguments.get(0);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments,state);

        LogMessageModel logMessageModel = logMessageHandler.processLogMessage(messageFormatArgument,
                remainingArguments, state, throwableArgument, migrationContext);
        builder.logMessageModel( logMessageModel );
        return builder.build();
    }

    @Nullable
    private ExpressionTree findThrowableArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        Optional<MatchResult> optionalThrownMatchResult = matchAtIndex(arguments, state, TinyLog2Matchers.throwableType(), 0);
        return optionalThrownMatchResult.map(MatchResult::argument).orElse(null);
    }
}