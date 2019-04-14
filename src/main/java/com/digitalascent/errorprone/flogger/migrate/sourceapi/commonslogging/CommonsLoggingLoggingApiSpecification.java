package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

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
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModelFactory;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.logFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggingMethod;

public class CommonsLoggingLoggingApiSpecification extends AbstractLoggingApiSpecification {

    private static final ImmutableSet<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.apache.commons.logging");

    public CommonsLoggingLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
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
        return logFactoryMethod().matches(variableTree.getInitializer(), visitorState);
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

        String level = methodInvocation.methodName().substring(2).replace("Enabled", "");
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
        LogMessage logMessage = createLogMessageModel(messageFormatArgument, argumentParser.remainingArguments(),
                methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel(logMessage);
        return builder.build();
    }
}
