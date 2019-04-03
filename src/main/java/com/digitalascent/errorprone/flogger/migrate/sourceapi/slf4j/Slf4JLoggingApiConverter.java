package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiConverter;
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

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.markerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerImports;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * SLF4J API: https://www.slf4j.org/apidocs/index.html
 */
public final class Slf4JLoggingApiConverter extends AbstractLoggingApiConverter {
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public Slf4JLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        super(floggerSuggestedFixGenerator,targetLogLevelFunction);
        this.floggerSuggestedFixGenerator = requireNonNull(floggerSuggestedFixGenerator, "floggerSuggestedFixGenerator");
        this.targetLogLevelFunction = requireNonNull(targetLogLevelFunction, "");
    }

    @Override
    public Optional<SuggestedFix> migrateLoggingMethodInvocation(MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {

        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(methodInvocationTree);
        String methodName = sym.getSimpleName().toString();
        if (loggingMethod().matches(methodInvocationTree, state)) {
            TargetLogLevel targetLogLevel = targetLogLevelFunction.apply(methodName);
            return Optional.of(migrateLoggingMethod(targetLogLevel, methodInvocationTree, state, migrationContext));
        }

        if (loggingEnabledMethod().matches(methodInvocationTree, state)) {
            String level = methodName.substring(2).replace("Enabled", "");
            TargetLogLevel targetLogLevel = targetLogLevelFunction.apply(level);
            return Optional.of(migrateConditionalMethod(targetLogLevel, methodInvocationTree, state, migrationContext));
        }

        return Optional.empty();
    }

    @Override
    protected boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return loggerFactoryMethod().matches(variableTree.getInitializer(), visitorState);
    }

    @Override
    public boolean isLoggerVariable(VariableTree tree, VisitorState state) {
        return loggerType().matches(tree, state);
    }

    @Override
    public Optional<SuggestedFix> migrateImport(ImportTree importTree, VisitorState visitorState) {
        if (loggerImports().matches(importTree.getQualifiedIdentifier(), visitorState)) {
            return Optional.of(floggerSuggestedFixGenerator.removeImport(importTree));
        }

        return Optional.empty();
    }

    private SuggestedFix migrateConditionalMethod(TargetLogLevel targetLogLevel, MethodInvocationTree tree, VisitorState state, MigrationContext migrationContext) {
        return floggerSuggestedFixGenerator.generateConditional(tree, state, targetLogLevel, migrationContext);
    }

    private SuggestedFix migrateLoggingMethod(TargetLogLevel targetLogLevel, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        ImmutableFloggerLogContext.Builder builder = ImmutableFloggerLogContext.builder();
        builder.targetLogLevel(targetLogLevel);

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();

        if( hasMarkerArgument(remainingArguments,state)) {
            remainingArguments = Arguments.removeFirst(remainingArguments);
        }

        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state );

        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, state);
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast( remainingArguments );
            builder.thrown(throwableArgument);
        }

        LogMessageModel logMessageModel = new Slf4jLogMessageHandler().processLogMessage(messageFormatArgument, remainingArguments, state, throwableArgument, migrationContext);
        builder.logMessageModel(logMessageModel);

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }

    private ExpressionTree findMessageFormatArgument(List<? extends ExpressionTree> arguments) {
        if( arguments.isEmpty() ) {
            throw new IllegalStateException("Unable to locate required message format argument");
        }
        return arguments.get(0);
    }

    private boolean hasMarkerArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        if( arguments.isEmpty() ) {
            return false;
        }
        return markerType().matches(arguments.get(0),state);
    }

}
