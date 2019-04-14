package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2;

import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModelFactory;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
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

public final class Log4j2LoggingApiSpecification extends AbstractLoggingApiSpecification {

    private static final Set<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.apache.logging.log4j");

    public Log4j2LoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
                                         LogMessageModelFactory logMessageModelFactory) {
        super(targetLogLevelFunction, logMessageModelFactory);
    }

    @Override
    public boolean matchConditionalMethod(ExpressionTree expressionTree, VisitorState state) {
        return loggingEnabledMethod().matches(expressionTree, state);
    }

    @Override
    public boolean matchLoggingMethod(ExpressionTree expressionTree, VisitorState state) {
        return loggingMethod().matches(expressionTree, state);
    }

    @Override
    public Set<String> loggingPackagePrefixes() {
        return LOGGING_PACKAGE_PREFIXES;
    }

    @Override
    public boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return logManagerMethod().matches(variableTree.getInitializer(), visitorState);
    }

    @Override
    public boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    @Override
    public FloggerConditionalStatement parseLoggingConditionalMethod(MethodInvocation methodInvocation) {
        ImmutableFloggerConditionalStatement.Builder builder = ImmutableFloggerConditionalStatement.builder();
        builder.targetLogLevel(determineTargetLogLevel(methodInvocation));
        builder.conditionalStatement(methodInvocation);
        return builder.build();
    }

    private TargetLogLevel determineTargetLogLevel(MethodInvocation methodInvocation) {
        TargetLogLevel targetLogLevel;
        if (methodInvocation.methodName().equals("isEnabled")) {
            targetLogLevel = resolveLogLevelFromArgument(methodInvocation.tree().getArguments().get(0));
        } else {
            String level = methodInvocation.methodName().substring(2).replace("Enabled", "");
            targetLogLevel = mapLogLevel(level);
        }
        return targetLogLevel;
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

    @Override
    public FloggerLogStatement parseLoggingMethod(MethodInvocation methodInvocation,
                                                  MigrationContext migrationContext) {

        List<? extends ExpressionTree> remainingArguments = methodInvocation.tree().getArguments();
        TargetLogLevel targetLogLevel;
        if (methodInvocation.methodName().equals("log")) {
            ExpressionTree logLevelArgument = remainingArguments.get(0);
            targetLogLevel = resolveLogLevelFromArgument(logLevelArgument);
            remainingArguments = Arguments.removeFirst(remainingArguments);
        } else {
            targetLogLevel = mapLogLevel(methodInvocation.methodName());
        }

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // skip marker object, if present
        remainingArguments = maybeSkipMarkerArgument(methodInvocation.state(), remainingArguments);

        // extract message format argument and it's arguments
        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, methodInvocation.state());

        // extract tailing exception, if present
        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, methodInvocation.state());
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast(remainingArguments);
            builder.thrown(throwableArgument);
        }

        LogMessageModel logMessageModel = createLogMessageModel(messageFormatArgument,
                remainingArguments, methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
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
