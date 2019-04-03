package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog;

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
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.throwableType;

/**
 * Tiny Log API: https://static.javadoc.io/org.tinylog/tinylog/1.3.6/index.html
 */
public final class TinyLogLoggingApiConverter extends AbstractLoggingApiConverter {
    public TinyLogLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        super( floggerSuggestedFixGenerator, targetLogLevelFunction);
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
    protected SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
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

        LogMessageModel logMessageModel = new TinyLogLogMessageHandler().processLogMessage(messageFormatArgument,
                remainingArguments, state, throwableArgument, migrationContext);
        builder.logMessageModel(logMessageModel);

        return getFloggerSuggestedFixGenerator().generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }

    @Nullable
    private ExpressionTree findThrowableArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        Optional<MatchResult> optionalThrownMatchResult = matchAtIndex(arguments, state, throwableType(), 0);
        return optionalThrownMatchResult.map(MatchResult::argument).orElse(null);
    }
}