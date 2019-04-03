package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

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
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.List;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.logFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.logType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingMatchers.loggingMethod;

/**
 * Commons Logging API: https://commons.apache.org/proper/commons-logging/apidocs/index.html
 */
public final class CommonsLoggingApiConverter extends AbstractLoggingApiConverter {

    private CommonsLoggingLogMessageHandler logMessageHandler = new CommonsLoggingLogMessageHandler();

    public CommonsLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        super(floggerSuggestedFixGenerator, targetLogLevelFunction);
    }

    @Override
    protected SuggestedFix migrateLoggingEnabledMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        String level = methodName.substring(2).replace("Enabled", "");
        targetLogLevel = mapLogLevel(level);
        return getFloggerSuggestedFixGenerator().generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
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
    protected boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState) {
        return logFactoryMethod().matches(variableTree.getInitializer(), visitorState);
    }

    @Override
    public boolean isLoggerVariable(VariableTree tree, VisitorState state) {
        return logType().matches(tree, state);
    }

    @Override
    protected boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState) {
        return loggerImports().matches(qualifiedIdentifier, visitorState);
    }

    protected SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        targetLogLevel = mapLogLevel(methodName);

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

        LogMessageModel logMessageModel = logMessageHandler.processLogMessage(messageFormatArgument, remainingArguments, state, throwableArgument, migrationContext);
        builder.logMessageModel(logMessageModel);

        return getFloggerSuggestedFixGenerator().generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }
}