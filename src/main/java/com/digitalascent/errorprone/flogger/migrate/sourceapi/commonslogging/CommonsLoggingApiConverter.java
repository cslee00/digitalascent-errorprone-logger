package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.classType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.logFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.logType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggingMethod;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Commons Logging API: https://commons.apache.org/proper/commons-logging/apidocs/index.html
 */
public final class CommonsLoggingApiConverter implements LoggingApiConverter {
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public CommonsLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        this.floggerSuggestedFixGenerator = requireNonNull(floggerSuggestedFixGenerator, "floggerSuggestedFixGenerator");
        this.targetLogLevelFunction = requireNonNull(targetLogLevelFunction, "");
    }

    @Override
    public Optional<SuggestedFix> migrateLoggingMethodInvocation(MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {

        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(methodInvocationTree);
        String methodName = sym.getSimpleName().toString();
        if (loggingMethod().matches(methodInvocationTree, state)) {
            return Optional.of(migrateLoggingMethod(methodName, methodInvocationTree, state, migrationContext));
        }

        if (loggingEnabledMethod().matches(methodInvocationTree, state)) {
            return Optional.of(migrateConditionalMethod(methodName, methodInvocationTree, state, migrationContext));
        }

        return Optional.empty();
    }

    @Override
    public Optional<SuggestedFix> migrateLoggerVariable(ClassTree classTree, VariableTree variableTree,
                                                        VisitorState state, MigrationContext migrationContext) {
        checkArgument(isLoggerVariable(variableTree, state), "isLoggerVariable(variableTree, state) : %s", variableTree);

        if (!logFactoryMethod().matches(variableTree.getInitializer(), state)) {
            return Optional.empty();
        }

        MethodInvocationTree logManagerMethodInvocationTree = (MethodInvocationTree) variableTree.getInitializer();
        if( Arguments.isLoggerNamedAfterClass(classTree, logManagerMethodInvocationTree.getArguments().get(0), state )) {
            return Optional.of(floggerSuggestedFixGenerator.generateLoggerVariable(classTree, variableTree, state, migrationContext));
        }

        return Optional.empty();
    }


    @Override
    public boolean isLoggerVariable(VariableTree tree, VisitorState state) {
        return logType().matches(tree, state);
    }

    @Override
    public Optional<SuggestedFix> migrateImport(ImportTree importTree, VisitorState visitorState) {
        if (loggerImports().matches(importTree.getQualifiedIdentifier(), visitorState)) {
            return Optional.of(floggerSuggestedFixGenerator.removeImport(importTree));
        }

        return Optional.empty();
    }

    private SuggestedFix migrateConditionalMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                  VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        String level = methodName.substring(2).replace("Enabled", "");
        targetLogLevel = targetLogLevelFunction.apply(level);
        return floggerSuggestedFixGenerator.generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
    }

    private SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                              VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        targetLogLevel = targetLogLevelFunction.apply(methodName);

        ImmutableFloggerLogContext.Builder builder = ImmutableFloggerLogContext.builder();
        builder.targetLogLevel(targetLogLevel);

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();
        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, state);
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast(remainingArguments);
            builder.thrown(throwableArgument);
        }

        ExpressionTree messageFormatArgument = remainingArguments.isEmpty() ? throwableArgument : remainingArguments.get(0);
        remainingArguments = Arguments.removeFirst(remainingArguments);

        LogMessageModel logMessageModel = new CommonsLoggingLogMessageHandler().processLogMessage(messageFormatArgument, remainingArguments, state, throwableArgument, migrationContext);
        builder.logMessageModel(logMessageModel);

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }
}