package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2;

import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageHandler;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.logManagerMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.markerType;

/**
 * Log4J2 API: https://logging.apache.org/log4j/2.x/log4j-api/apidocs/index.html
 */
public final class Log4j2LoggingApiConverter extends AbstractLoggingApiConverter {

    private static final Set<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.apache.logging.log4j");

    public Log4j2LoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                     Function<String, TargetLogLevel> targetLogLevelFunction,
                                     LogMessageHandler logMessageHandler) {
        super( floggerSuggestedFixGenerator, targetLogLevelFunction, logMessageHandler);
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
        if (methodName.equals("isEnabled")) {
            targetLogLevel = resolveLogLevelFromArgument(methodInvocationTree.getArguments().get(0));
        } else {
            String level = methodName.substring(2).replace("Enabled", "");
            targetLogLevel = mapLogLevel(level);
        }
        return getFloggerSuggestedFixGenerator().generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
    }

    @Override
    protected Set<String> loggingPackagePrefixes() {
        return LOGGING_PACKAGE_PREFIXES;
    }

    @Override
    protected boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return logManagerMethod().matches(variableTree.getInitializer(), visitorState);
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

    protected FloggerLogStatement migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                       VisitorState state, MigrationContext migrationContext) {

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();
        TargetLogLevel targetLogLevel;
        if (methodName.equals("log")) {
            ExpressionTree logLevelArgument = remainingArguments.get(0);
            targetLogLevel = resolveLogLevelFromArgument(logLevelArgument);
            remainingArguments = Arguments.removeFirst(remainingArguments);
        } else {
            targetLogLevel = mapLogLevel(methodName);
        }

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // skip marker object, if present
        remainingArguments = maybeSkipMarkerArgument(state, remainingArguments);

        // extract message format argument and it's arguments
        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state);

        // extract tailing exception, if present
        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, state);
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast(remainingArguments);
            builder.thrown(throwableArgument);
        }

        LogMessageModel logMessageModel = createLogMessageModel(messageFormatArgument,
                remainingArguments, state, throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel(logMessageModel);
        return builder.build();
    }

    private List<? extends ExpressionTree> maybeSkipMarkerArgument(VisitorState state, List<? extends ExpressionTree> remainingArguments) {
        if (hasMarkerArgument(remainingArguments, state)) {
            remainingArguments = Arguments.removeFirst(remainingArguments);
        }
        return remainingArguments;
    }

    private boolean hasMarkerArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        if (arguments.isEmpty()) {
            return false;
        }
        return markerType().matches(arguments.get(0), state);
    }

    private ExpressionTree findMessageFormatArgument(List<? extends ExpressionTree> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalStateException("Unable to find required message format argument");
        }
        return arguments.get(0);
    }
}
