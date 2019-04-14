package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j;

import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.ArgumentParser;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModelFactory;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;

import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.logManagerMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggingMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

public final class Log4JLoggingApiSpecification extends AbstractLoggingApiSpecification {
    private static final ImmutableSet<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.apache.log4j");

    public Log4JLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
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
    public boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return logManagerMethod().matches(variableTree.getInitializer(), visitorState);
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
        builder.targetLogLevel(determineTargetLogLevel(methodInvocation));
        builder.conditionalStatement(methodInvocation);
        return builder.build();
    }

    private TargetLogLevel determineTargetLogLevel(MethodInvocation methodInvocation) {
        TargetLogLevel targetLogLevel;
        if (methodInvocation.methodName().equals("isEnabledFor")) {
            targetLogLevel = resolveLogLevelFromArgument(methodInvocation.tree().getArguments().get(0));
        } else {
            String level = parseLevelFromMethodName(methodInvocation);
            targetLogLevel = mapLogLevel(level);
        }
        return targetLogLevel;
    }

    private String parseLevelFromMethodName(MethodInvocation methodInvocation) {
        return methodInvocation.methodName().substring(2).replace("Enabled", "");
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

        ArgumentParser argumentParser = ArgumentParser.forArgumentsOf(methodInvocation);

        TargetLogLevel targetLogLevel = determineTargetLogLevel(methodInvocation, argumentParser);

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // extract message format extract
        ExpressionTree messageFormatArgument = argumentParser.extract();

        // extract throwable as last extract, if present
        ExpressionTree throwableArgument = argumentParser.trailingThrowable();
        builder.thrown(throwableArgument);

        LogMessage logMessage = createLogMessageModel(messageFormatArgument,
                argumentParser.remainingArguments(), methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel(logMessage);
        return builder.build();
    }

    private TargetLogLevel determineTargetLogLevel(MethodInvocation methodInvocation, ArgumentParser argumentParser) {
        TargetLogLevel targetLogLevel;
        if (methodInvocation.methodName().equals("log")) {
            ExpressionTree logLevelArgument = argumentParser.firstMatching(
                    argument -> isSubtypeOf("org.apache.log4j.Priority").matches(argument, methodInvocation.state()));
            targetLogLevel = resolveLogLevelFromArgument(logLevelArgument);
        } else {
            targetLogLevel = mapLogLevel(methodInvocation.methodName());
        }
        return targetLogLevel;
    }
}
