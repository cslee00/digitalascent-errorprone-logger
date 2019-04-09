package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog;

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
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.throwableType;

/**
 * Tiny Log API: https://static.javadoc.io/org.tinylog/tinylog/1.3.6/index.html
 */
public final class TinyLogLoggingApiConverter extends AbstractLoggingApiConverter {

    private static final Set<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.pmw.tinylog");

    public TinyLogLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                      Function<String, TargetLogLevel> targetLogLevelFunction,
                                      LogMessageHandler logMessageHandler) {
        super(floggerSuggestedFixGenerator, targetLogLevelFunction, logMessageHandler);
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

        // extract throwable as first parameter, if present
        ExpressionTree throwableArgument = findThrowableArgument(remainingArguments, state);
        if (throwableArgument != null) {
            builder.thrown(throwableArgument);
            remainingArguments = Arguments.removeFirst(remainingArguments);
        }

        // extract message format argument, if present
        ExpressionTree messageFormatArgument = remainingArguments.isEmpty() ? throwableArgument : remainingArguments.get(0);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state);

        LogMessageModel logMessageModel = createLogMessageModel(messageFormatArgument,
                remainingArguments, state, throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel(logMessageModel);
        return builder.build();
    }

    @Nullable
    private ExpressionTree findThrowableArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        Optional<MatchResult> optionalThrownMatchResult = matchAtIndex(arguments, state, throwableType(), 0);
        return optionalThrownMatchResult.map(MatchResult::argument).orElse(null);
    }
}