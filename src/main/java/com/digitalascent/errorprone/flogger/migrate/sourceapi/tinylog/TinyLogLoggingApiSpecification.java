package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog;

import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModelFactory;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MatchResult;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments.matchAtIndex;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogMatchers.throwableType;

public final class TinyLogLoggingApiSpecification extends AbstractLoggingApiSpecification {
    private static final Set<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("org.pmw.tinylog");

    public TinyLogLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
                                          LogMessageModelFactory logMessageModelFactory) {
        super(targetLogLevelFunction, logMessageModelFactory);
    }


    @Override
    public boolean matchConditionalMethod(ExpressionTree expressionTree, VisitorState state) {
        return false;
    }

    @Override
    public boolean matchLoggingMethod(ExpressionTree expressionTree, VisitorState state) {
        return loggingMethod().matches(expressionTree, state);
    }

    @Override
    public boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return false;
    }

    @Override
    public FloggerConditionalStatement parseLoggingConditionalMethod(MethodInvocation methodInvocation,
                                                                     VisitorState state,
                                                                     MigrationContext migrationContext) {
        throw new UnsupportedOperationException("TinyLog doesn't have logging enabled methods");
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
    public FloggerLogStatement parseLoggingMethod(MethodInvocation methodInvocation,
                                                  VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel = mapLogLevel(methodInvocation.methodName());
        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        List<? extends ExpressionTree> remainingArguments = methodInvocation.tree().getArguments();

        // extract throwable as first parameter, if present
        ExpressionTree throwableArgument = findThrowableArgument(remainingArguments, state);
        if (throwableArgument != null) {
            builder.thrown(throwableArgument);
            remainingArguments = Arguments.removeFirst(remainingArguments);
        }

        // extract message format argument, if present
        ExpressionTree messageFormatArgument = remainingArguments.isEmpty() ? throwableArgument : remainingArguments.get(0);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state);

        LogMessageModel logMessageModel = createLogMessageModel(messageFormatArgument,
                remainingArguments, state, throwableArgument, migrationContext, targetLogLevel);
        builder.logMessageModel(logMessageModel);
        return builder.build();
    }

    @Nullable
    private ExpressionTree findThrowableArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        Optional<MatchResult> optionalThrownMatchResult = matchAtIndex(arguments, state, throwableType(), 0);
        return optionalThrownMatchResult.map(MatchResult::argument).orElse(null);
    }
}
