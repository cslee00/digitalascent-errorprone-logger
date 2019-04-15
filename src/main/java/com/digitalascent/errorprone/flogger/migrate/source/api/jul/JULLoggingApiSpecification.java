package com.digitalascent.errorprone.flogger.migrate.source.api.jul;

import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.Arguments;
import com.digitalascent.errorprone.flogger.migrate.source.api.AbstractLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.ArgumentParser;
import com.digitalascent.errorprone.flogger.migrate.source.api.LogMessageFactory;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.source.api.jul.JULMatchers.logLevelType;
import static com.digitalascent.errorprone.flogger.migrate.source.api.jul.JULMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.source.api.jul.JULMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.source.api.jul.JULMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.source.api.jul.JULMatchers.loggingMethod;

/**
 * Java Logging API: https://docs.oracle.com/javase/8/docs/api/java/util/logging/Logger.html
 */
public final class JULLoggingApiSpecification extends AbstractLoggingApiSpecification {
    private static final ImmutableSet<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("java.util.logging");
    private static final Set<String> ENTERING_EXITING_METHOD_NAMES = ImmutableSet.of("entering", "exiting");

    public JULLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
                                      LogMessageFactory logMessageFactory) {
        super(targetLogLevelFunction, logMessageFactory);
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
    public boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return loggerFactoryMethod().matches(variableTree.getInitializer(), visitorState);
    }

    @Override
    public boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    @Override
    public Set<String> loggingPackagePrefixes() {
        return LOGGING_PACKAGE_PREFIXES;
    }

    @Override
    public FloggerConditionalStatement parseConditionalMethod(MethodInvocation methodInvocation) {
        ImmutableFloggerConditionalStatement.Builder builder = ImmutableFloggerConditionalStatement.builder();
        builder.targetLogLevel(resolveLogLevelFromArgument(methodInvocation.firstArgument()));
        builder.conditionalStatement(methodInvocation);
        return builder.build();
    }

    private TargetLogLevel resolveLogLevelFromArgument(ExpressionTree levelArgument) {
        try {
            if (levelArgument instanceof JCTree.JCFieldAccess) {
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) levelArgument;
                if (fieldAccess.selected.type.toString().equals("java.util.logging.Level")) {
                    return mapLogLevel(fieldAccess.name.toString());
                }
            }
            return TargetLogLevel.customLogLevel(levelArgument);
        } catch (IllegalArgumentException ignored) {
        }
        throw new SkipCompilationUnitException("Custom log level not supported: " + levelArgument);
    }

    @Override
    public FloggerLogStatement parseLoggingMethod(MethodInvocation methodInvocation,
                                                  MigrationContext migrationContext) {

        if ("throwing".equals(methodInvocation.methodName())) {
            return parseThrowing(methodInvocation, migrationContext);
        }

        if (ENTERING_EXITING_METHOD_NAMES.contains(methodInvocation.methodName())) {
            return parseEnteringExiting(methodInvocation, migrationContext);
        }

        return parseLog(methodInvocation, migrationContext);
    }

    private FloggerLogStatement parseLog(MethodInvocation methodInvocation, MigrationContext migrationContext) {
        ArgumentParser argumentParser = ArgumentParser.forArgumentsOf(methodInvocation);

        TargetLogLevel targetLogLevel = determineTargetLogLevel(methodInvocation, argumentParser);

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // extract message format extract and it's arguments
        ExpressionTree messageFormatArgument = argumentParser.extract();
        argumentParser.maybeUnpackVarArgs();

        // extract throwable extract at the end, if present
        ExpressionTree throwableArgument = argumentParser.trailingThrowable();
        builder.thrown(throwableArgument);

        LogMessage logMessage = createLogMessage(messageFormatArgument, argumentParser.remainingArguments(),
                methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
        builder.logMessage(logMessage);
        return builder.build();
    }

    private FloggerLogStatement parseEnteringExiting(MethodInvocation methodInvocation, MigrationContext migrationContext) {
        // "entering" and "exiting" log at finer level
        TargetLogLevel targetLogLevel = mapLogLevel("finer");
        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        ArgumentParser argumentParser = ArgumentParser.forArgumentsOf(methodInvocation);

        // entering(String sourceClass, String sourceMethod, Object param1)
        // skip sourceClass, sourceMethod arguments
        argumentParser.skip(2);

        String msgPrefix = determineEnteringExitingPrefix(methodInvocation.methodName());
        StringBuilder sb = new StringBuilder();
        sb.append(msgPrefix);
        List<? extends ExpressionTree> args = argumentParser.remainingArguments();
        if (!argumentParser.isEmpty()) {
            args = Arguments.maybeUnpackVarArgs(args, methodInvocation.state());
            for (int i = 0; i < args.size(); i++) {
                sb.append(" %s");
            }
        }
        LogMessage logMessage = createLogMessage(sb.toString(), args, methodInvocation.state(), targetLogLevel);
        builder.logMessage(logMessage);
        return builder.build();
    }

    private String determineEnteringExitingPrefix(String methodName) {
        return methodName.equals("entering") ? "ENTRY" : "RETURN";
    }

    private FloggerLogStatement parseThrowing(MethodInvocation methodInvocation, MigrationContext migrationContext) {

        // "throwing" logs at finer level
        TargetLogLevel targetLogLevel = mapLogLevel("finer");

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // throwing(String sourceClass, String sourceMethod, Throwable thrown)
        ExpressionTree throwableArgument = methodInvocation.tree().getArguments().get(2);
        builder.thrown(throwableArgument);

        builder.logMessage(LogMessage.fromStringFormat("THROW", ImmutableList.of()));
        return builder.build();
    }

    private TargetLogLevel determineTargetLogLevel(MethodInvocation methodInvocation, ArgumentParser argumentParser) {
        TargetLogLevel targetLogLevel;
        if (methodInvocation.methodName().equals("log")) {
            ExpressionTree logLevelArgument = argumentParser.extract();
            if (logLevelType().matches(logLevelArgument, methodInvocation.state())) {
                targetLogLevel = resolveLogLevelFromArgument(logLevelArgument);
            } else {
                throw new SkipLogMethodException("Unable to determine log level");
            }
        } else {
            targetLogLevel = mapLogLevel(methodInvocation.methodName());
        }
        return targetLogLevel;
    }
}
