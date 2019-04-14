package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

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

import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.markerType;
import static com.google.common.base.Preconditions.checkArgument;

public final class Slf4jLoggingApiSpecification extends AbstractLoggingApiSpecification {
    private static final Set<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.slf4j");

    public Slf4jLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
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
        builder.targetLogLevel(mapLogLevel(parseLevelFromMethodName(methodInvocation)));
        builder.conditionalStatement(methodInvocation);
        return builder.build();
    }

    private String parseLevelFromMethodName(MethodInvocation methodInvocation) {
        return methodInvocation.methodName().substring(2).replace("Enabled", "");
    }

    @Override
    public FloggerLogStatement parseLoggingMethod(MethodInvocation methodInvocation, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel = mapLogLevel(methodInvocation.methodName());
        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        ArgumentParser argumentParser = ArgumentParser.forArgumentsOf(methodInvocation);

        argumentParser.skipIfPresent( argument -> markerType().matches(argument, methodInvocation.state()));
        ExpressionTree messageFormatArgument = argumentParser.extract();
        argumentParser.maybeUnpackVarArgs();

        ExpressionTree throwableArgument = argumentParser.trailingThrowable();
        builder.thrown(throwableArgument);

        LogMessage logMessage = createLogMessageModel(messageFormatArgument, argumentParser.remainingArguments(),
                methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel(logMessage);

        return builder.build();
    }
}
