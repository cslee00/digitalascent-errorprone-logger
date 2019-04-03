package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
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
import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.logManagerMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggingMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

/**
 * Log4J API: https://logging.apache.org/log4j/1.2/apidocs/index.html
 */
public final class Log4jLoggingApiConverter extends AbstractLoggingApiConverter {

    private Log4jLogMessageHandler logMessageHandler = new Log4jLogMessageHandler();

    public Log4jLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        super( floggerSuggestedFixGenerator, targetLogLevelFunction);
    }

    @Override
    protected boolean matchLoggingEnabledMethod(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return loggingEnabledMethod().matches(methodInvocationTree, state);
    }

    @Override
    protected boolean matchLoggingMethod(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return loggingMethod().matches(methodInvocationTree, state);
    }

    @Override
    protected SuggestedFix migrateLoggingEnabledMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        if (methodName.equals("isEnabledFor")) {
            targetLogLevel = resolveLogLevelFromArgument(methodInvocationTree.getArguments().get(0));
        } else {
            String level = methodName.substring(2).replace("Enabled", "");
            targetLogLevel = mapLogLevel(level);
        }
        return getFloggerSuggestedFixGenerator().generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
    }

    @Override
    protected boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return logManagerMethod().matches(variableTree.getInitializer(), visitorState);
    }

    @Override
    public boolean isLoggerVariable(VariableTree tree, VisitorState state) {
        return loggerType().matches(tree, state);
    }

    @Override
    protected boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    private TargetLogLevel resolveLogLevelFromArgument(ExpressionTree levelArgument) {
        try {
            if (levelArgument instanceof JCTree.JCFieldAccess) {
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) levelArgument;
                return mapLogLevel(fieldAccess.name.toString());
            }
        } catch (IllegalArgumentException ignored) {
        }
        throw new SkipCompilationUnitException("Custom log level not supported: " + levelArgument);
    }

    protected SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                VisitorState state, MigrationContext migrationContext) {

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();
        TargetLogLevel targetLogLevel;
        if (methodName.equals("log")) {
            ExpressionTree logLevelArgument = findLogLevelArgument(remainingArguments, state);
            targetLogLevel = resolveLogLevelFromArgument(logLevelArgument);
            remainingArguments = Arguments.findRemainingAfter(remainingArguments, state, logLevelArgument);
        } else {
            targetLogLevel = mapLogLevel(methodName);
        }

        ImmutableFloggerLogContext.Builder builder = ImmutableFloggerLogContext.builder();
        builder.targetLogLevel(targetLogLevel);

        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments);
        remainingArguments = Arguments.removeFirst(remainingArguments);

        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, state);
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast(remainingArguments);
            builder.thrown(throwableArgument);
        }

        LogMessageModel logMessageModel = logMessageHandler.processLogMessage(messageFormatArgument,
                remainingArguments, state, throwableArgument, migrationContext);
        builder.logMessageModel(logMessageModel);

        return getFloggerSuggestedFixGenerator().generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }

    private ExpressionTree findMessageFormatArgument(List<? extends ExpressionTree> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalStateException("Unable to find required message format argument");
        }
        return arguments.get(0);
    }

    private ExpressionTree findLogLevelArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        Optional<MatchResult> optionalArgumentMatchResult =
                Arguments.firstMatching(arguments, state, isSubtypeOf("org.apache.log4j.Priority"));
        MatchResult matchResult = optionalArgumentMatchResult.orElseThrow(() -> new IllegalArgumentException("Unable to locate required Priority parameter"));
        if (matchResult.index() > 1) {
            throw new IllegalArgumentException("Unable to locate required Priority parameter");
        }
        return matchResult.argument();
    }
}
