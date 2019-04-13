package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public abstract class AbstractLoggingApiConverter implements LoggingApiConverter {
    private static final SuggestedFix EMPTY = SuggestedFix.builder().build();
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;
    private final LogMessageHandler logMessageHandler;

    protected AbstractLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                          Function<String, TargetLogLevel> targetLogLevelFunction,
                                          LogMessageHandler logMessageHandler) {
        this.floggerSuggestedFixGenerator = requireNonNull(floggerSuggestedFixGenerator, "floggerSuggestedFixGenerator");
        this.targetLogLevelFunction = requireNonNull(targetLogLevelFunction, "targetLogLevelFunction");
        this.logMessageHandler = requireNonNull(logMessageHandler, "logMessageHandler");
    }

    @Override
    public final SuggestedFix migrateImport(ImportTree importTree, VisitorState visitorState) {
        if (matchImport(importTree.getQualifiedIdentifier(), visitorState)) {
            return floggerSuggestedFixGenerator.removeImport(importTree);
        }

        if (loggingPackagePrefixes().stream()
                .anyMatch(x -> importTree.getQualifiedIdentifier().toString().startsWith(x))) {
            return floggerSuggestedFixGenerator.removeImport(importTree);
        }
        return EMPTY;
    }

    @Override
    public final LoggerVariableNamingType determineLoggerVariableNamingType(ClassTree classTree, VariableTree variableTree, VisitorState visitorState) {
        if (!matchLogFactory(variableTree, visitorState)) {
            return LoggerVariableNamingType.NOT_LOGGER;
        }

        MethodInvocationTree logManagerMethodInvocationTree = (MethodInvocationTree) variableTree.getInitializer();
        if (logManagerMethodInvocationTree.getArguments().isEmpty() || Arguments.isLoggerNamedAfterClass(classTree,
                logManagerMethodInvocationTree.getArguments().get(0), visitorState)) {
            return LoggerVariableNamingType.CLASS_NAMED;
        }

        return LoggerVariableNamingType.NON_CLASS_NAMED;
    }

    @Override
    public final SuggestedFix migrateLoggingMethodInvocation(MethodInvocationTree loggingMethodInvocation, VisitorState state, MigrationContext migrationContext) {
        checkArgument(matchLoggingMethod(loggingMethodInvocation, state), "matchLoggingMethod(loggingMethodInvocation, state) : %s", loggingMethodInvocation);

        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(loggingMethodInvocation);
        String methodName = sym.getSimpleName().toString();

        FloggerLogStatement floggerLogStatement = migrateLoggingMethod(methodName,
                loggingMethodInvocation, state, migrationContext);
        return getFloggerSuggestedFixGenerator().generateLoggingMethod(loggingMethodInvocation,
                state, floggerLogStatement, migrationContext);

    }

    @Override
    public final SuggestedFix migrateLoggingEnabledMethodInvocation(MethodInvocationTree loggingEnabledMethodInvocation, VisitorState state, MigrationContext migrationContext) {
        checkArgument(matchLoggingEnabledMethod(loggingEnabledMethodInvocation, state),
                "matchLoggingMethod(loggingEnabledMethodInvocation, state) : %s", loggingEnabledMethodInvocation);

        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(loggingEnabledMethodInvocation);
        String methodName = sym.getSimpleName().toString();

        return migrateLoggingEnabledMethod(methodName, loggingEnabledMethodInvocation, state, migrationContext);
    }

    protected final LogMessageModel createLogMessageModel(ExpressionTree messageFormatArgument,
                                                          List<? extends ExpressionTree> remainingArguments,
                                                          VisitorState state,
                                                          @Nullable ExpressionTree thrownArgument,
                                                          MigrationContext migrationContext,
                                                          TargetLogLevel targetLogLevel) {
        return logMessageHandler.processLogMessage(messageFormatArgument, remainingArguments,
                state, thrownArgument, migrationContext, targetLogLevel);
    }

    protected final TargetLogLevel mapLogLevel(String level) {
        return targetLogLevelFunction.apply(level);
    }

    protected final FloggerSuggestedFixGenerator getFloggerSuggestedFixGenerator() {
        return floggerSuggestedFixGenerator;
    }

    protected abstract boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState);

    protected abstract Set<String> loggingPackagePrefixes();

    protected abstract boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState);

    protected abstract FloggerLogStatement migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext);

    protected abstract SuggestedFix migrateLoggingEnabledMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext);
}
