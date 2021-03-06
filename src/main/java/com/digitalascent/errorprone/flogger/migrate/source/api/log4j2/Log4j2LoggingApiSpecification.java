package com.digitalascent.errorprone.flogger.migrate.source.api.log4j2;

import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.ArgumentParser;
import com.digitalascent.errorprone.flogger.migrate.source.api.AbstractLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.LogMessageFactory;
import com.digitalascent.errorprone.flogger.migrate.source.api.SourceApiUtil;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;

import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.source.api.log4j2.Log4j2Matchers.logManagerMethod;
import static com.digitalascent.errorprone.flogger.migrate.source.api.log4j2.Log4j2Matchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.source.api.log4j2.Log4j2Matchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.source.api.log4j2.Log4j2Matchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.source.api.log4j2.Log4j2Matchers.markerType;

/**
 * Log4J2 API: https://logging.apache.org/log4j/2.x/log4j-api/apidocs/index.html
 */
public final class Log4j2LoggingApiSpecification extends AbstractLoggingApiSpecification {

    private static final Set<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.apache.logging.log4j");
    private static final Set<String> LOGGING_IMPORTS_TO_RETAIN = ImmutableSet.of("org.apache.logging.log4j.message.Message");

    public Log4j2LoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
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
    public boolean shouldRemoveImport(String importString) {
        if( LOGGING_IMPORTS_TO_RETAIN.contains(importString)) {
            return false;
        }

        return LOGGING_PACKAGE_PREFIXES.stream().anyMatch(importString::startsWith);
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
    public FloggerConditionalStatement parseConditionalMethod(MethodInvocation methodInvocation) {
        ImmutableFloggerConditionalStatement.Builder builder = ImmutableFloggerConditionalStatement.builder();
        builder.targetLogLevel(determineTargetLogLevel(methodInvocation));
        builder.conditionalStatement(methodInvocation);
        return builder.build();
    }

    private TargetLogLevel determineTargetLogLevel(MethodInvocation methodInvocation) {
        TargetLogLevel targetLogLevel;
        if (methodInvocation.methodName().equals("isEnabled")) {
            targetLogLevel = resolveLogLevelFromArgument(methodInvocation.firstArgument());
        } else {
            String level = SourceApiUtil.logLevelFromMethodName(methodInvocation);
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

    /*
     * TODO:
     *  entry()
     *  entry( Object... )
     *  exit()
     *  exit( Object result )
     *  traceEntry, traceExit
     *  throwing
     */
    @Override
    public FloggerLogStatement parseLoggingMethod(MethodInvocation methodInvocation,
                                                  MigrationContext migrationContext) {

        ArgumentParser argumentParser = ArgumentParser.forArgumentsOf(methodInvocation);
        TargetLogLevel targetLogLevel = determineTargetLogLevel(methodInvocation, argumentParser);

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // skip marker object, if present
        argumentParser.skipIfPresent(argument -> markerType().matches(argument, methodInvocation.state()));

        // extract message format extract and it's arguments
        ExpressionTree messageFormatArgument = argumentParser.extract();
        argumentParser.maybeUnpackVarArgs();

        // extract tailing exception, if present
        ExpressionTree throwableArgument = argumentParser.trailingThrowable();
        builder.thrown(throwableArgument);

        LogMessage logMessage = createLogMessage(messageFormatArgument,
                argumentParser.remainingArguments(), methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
        builder.logMessage(logMessage);
        return builder.build();
    }

    private TargetLogLevel determineTargetLogLevel(MethodInvocation methodInvocation, ArgumentParser argumentParser) {
        TargetLogLevel targetLogLevel;
        if (methodInvocation.methodName().equals("log") || methodInvocation.methodName().equals("printf")) {
            ExpressionTree logLevelArgument = argumentParser.extract();
            targetLogLevel = resolveLogLevelFromArgument(logLevelArgument);
        } else {
            targetLogLevel = mapLogLevel(methodInvocation.methodName());
        }
        return targetLogLevel;
    }
}
