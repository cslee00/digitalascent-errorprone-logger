package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2;

import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MatchResult;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments.matchAtIndex;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggingMethod;

/**
 * Tiny Log 2 API: https://tinylog.org/v2/javadoc/
 */
public final class TinyLog2LoggingApiConverter extends AbstractLoggingApiConverter {

    private static final Set<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.tinylog");

    public TinyLog2LoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                       Function<String, TargetLogLevel> targetLogLevelFunction,
                                       LogMessageHandler logMessageHandler) {
        super(floggerSuggestedFixGenerator, targetLogLevelFunction, logMessageHandler);
    }

    @Override
    public boolean matchLoggingEnabledMethod(ExpressionTree expressionTree, VisitorState state) {
        return false;
    }

    @Override
    public boolean matchLoggingMethod(ExpressionTree expressionTree, VisitorState state) {
        return loggingMethod().matches(expressionTree, state);
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
    protected boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    @Override
    protected Set<String> loggingPackagePrefixes() {
        return LOGGING_PACKAGE_PREFIXES;
    }

    @Override
    protected FloggerLogStatement migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                       VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel = mapLogLevel(methodName);
        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();
        ExpressionTree throwableArgument = findThrowableArgument(remainingArguments,state);
        if( throwableArgument != null ) {
            builder.thrown(throwableArgument);
            remainingArguments = Arguments.removeFirst( remainingArguments );
        }

        ExpressionTree messageFormatArgument = remainingArguments.isEmpty() ? throwableArgument : remainingArguments.get(0);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments,state);

        LogMessageModel logMessageModel = createLogMessageModel(messageFormatArgument,
                remainingArguments, state, throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel( logMessageModel );
        return builder.build();
    }

    @Nullable
    private ExpressionTree findThrowableArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        Optional<MatchResult> optionalThrownMatchResult = matchAtIndex(arguments, state, TinyLog2Matchers.throwableType(), 0);
        return optionalThrownMatchResult.map(MatchResult::argument).orElse(null);
    }
}