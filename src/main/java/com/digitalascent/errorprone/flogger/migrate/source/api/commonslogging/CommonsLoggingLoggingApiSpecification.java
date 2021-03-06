package com.digitalascent.errorprone.flogger.migrate.source.api.commonslogging;

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
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.source.api.commonslogging.CommonsLoggingMatchers.logFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.source.api.commonslogging.CommonsLoggingMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.source.api.commonslogging.CommonsLoggingMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.source.api.commonslogging.CommonsLoggingMatchers.loggingMethod;

/**
 * Commons Logging API: https://commons.apache.org/proper/commons-logging/apidocs/index.html
 */
public class CommonsLoggingLoggingApiSpecification extends AbstractLoggingApiSpecification {

    private static final ImmutableSet<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.apache.commons.logging");

    public CommonsLoggingLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
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
        return logFactoryMethod().matches(variableTree.getInitializer(), visitorState);
    }

    @Override
    public boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    @Override
    public boolean shouldRemoveImport(String importString) {
        return LOGGING_PACKAGE_PREFIXES.stream().anyMatch(importString::startsWith);
    }

    @Override
    public FloggerConditionalStatement parseConditionalMethod(MethodInvocation methodInvocation) {
        ImmutableFloggerConditionalStatement.Builder builder = ImmutableFloggerConditionalStatement.builder();

        String level = SourceApiUtil.logLevelFromMethodName(methodInvocation);
        builder.targetLogLevel(mapLogLevel(level));
        builder.conditionalStatement(methodInvocation);
        return builder.build();
    }

    @Override
    public FloggerLogStatement parseLoggingMethod(MethodInvocation methodInvocation,
                                                  MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel = mapLogLevel(methodInvocation.methodName());

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        ArgumentParser argumentParser = ArgumentParser.forArgumentsOf(methodInvocation);

        ExpressionTree throwableArgument = argumentParser.trailingThrowable();
        builder.thrown(throwableArgument);

        ExpressionTree messageFormatArgument = argumentParser.extractOrElse(throwableArgument);

        Verify.verify(messageFormatArgument != null);
        LogMessage logMessage = createLogMessage(messageFormatArgument, argumentParser.remainingArguments(),
                methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
        builder.logMessage(logMessage);
        return builder.build();
    }

}
