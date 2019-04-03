package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
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
import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.logLevelType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.stringType;

/**
 * JUL API: https://docs.oracle.com/javase/8/docs/api/java/util/logging/Logger.html
 */
public final class JULLoggingApiConverter extends AbstractLoggingApiConverter {

    private JULLogMessageHandler logMessageHandler = new JULLogMessageHandler();

    public JULLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        super( floggerSuggestedFixGenerator, targetLogLevelFunction);
    }

    @Override
    protected SuggestedFix migrateLoggingEnabledMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        targetLogLevel = resolveLogLevelFromArgument(methodInvocationTree.getArguments().get(0));
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

    protected ImmutableFloggerLogContext migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                              VisitorState state, MigrationContext migrationContext) {
        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();

        TargetLogLevel targetLogLevel;
        if (methodName.equals("log")) {
            ExpressionTree logLevelArgument = remainingArguments.get(0);
            if (logLevelType().matches(logLevelArgument, state)) {
                targetLogLevel = resolveLogLevelFromArgument(logLevelArgument);
                remainingArguments = Arguments.removeFirst(remainingArguments);
            } else {
                throw new SkipLogMethodException("Unable to determine log level");
            }
        } else {
            targetLogLevel = mapLogLevel(methodName);
        }

        ImmutableFloggerLogContext.Builder builder = ImmutableFloggerLogContext.builder();

        builder.targetLogLevel(targetLogLevel);

        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state);

        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, state);
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast(remainingArguments);
            builder.thrown(throwableArgument);
        }

        if (!stringType().matches(messageFormatArgument, state)) {
            throw new SkipLogMethodException("Unable to convert message format: " + messageFormatArgument);
        }

        LogMessageModel logMessageModel = logMessageHandler.processLogMessage(messageFormatArgument, remainingArguments, state, throwableArgument, migrationContext);
        builder.logMessageModel(logMessageModel);
        return builder.build();
    }

    private ExpressionTree findMessageFormatArgument(List<? extends ExpressionTree> arguments) {
        if (arguments.isEmpty()) {
            throw new IllegalStateException("Missing required message format argument");
        }
        return arguments.get(0);
    }
}
