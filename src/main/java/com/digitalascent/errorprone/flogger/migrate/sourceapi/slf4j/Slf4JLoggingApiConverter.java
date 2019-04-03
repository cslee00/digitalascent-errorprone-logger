package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.markerType;
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
    protected boolean matchLoggingEnabledMethod(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return loggingEnabledMethod().matches(methodInvocationTree, state);
    }

    @Override
    protected boolean matchLoggingMethod(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return loggingMethod().matches(methodInvocationTree, state);
    }

    @Override
    protected SuggestedFix migrateLoggingEnabledMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        String level = methodName.substring(2).replace("Enabled", "");
        TargetLogLevel targetLogLevel = targetLogLevelFunction.apply(level);
        return floggerSuggestedFixGenerator.generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
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
    protected boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    @Override
    protected SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel = targetLogLevelFunction.apply(methodName);
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
