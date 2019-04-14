package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

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
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.ArgumentParser;
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

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.logLevelType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggingMethod;

public final class JULLoggingApiSpecification extends AbstractLoggingApiSpecification {
    private static final ImmutableSet<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("java.util.logging");

    public JULLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
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
        builder.targetLogLevel(resolveLogLevelFromArgument(methodInvocation.tree().getArguments().get(0)));
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

        LogMessage logMessage = createLogMessageModel(messageFormatArgument, argumentParser.remainingArguments(),
                methodInvocation.state(), throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel(logMessage);
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
