package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j;

import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModelFactory;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MatchResult;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.Optional;
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
    public FloggerConditionalStatement parseLoggingConditionalMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        if (methodName.equals("isEnabledFor")) {
            targetLogLevel = resolveLogLevelFromArgument(methodInvocationTree.getArguments().get(0));
        } else {
            String level = methodName.substring(2).replace("Enabled", "");
            targetLogLevel = mapLogLevel(level);
        }

        ImmutableFloggerConditionalStatement.Builder builder = ImmutableFloggerConditionalStatement.builder();
        builder.targetLogLevel(targetLogLevel);
        builder.conditionalStatement(methodInvocationTree);
        return builder.build();
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
    public FloggerLogStatement parseLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
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

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // extract message format argument
        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments);
        remainingArguments = Arguments.removeFirst(remainingArguments);

        // extract throwable as last argument, if present
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
