package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableFloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageHandler;
import com.google.common.collect.ImmutableSet;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
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

/**
 * JUL API: https://docs.oracle.com/javase/8/docs/api/java/util/logging/Logger.html
 */
public final class JULLoggingApiConverter extends AbstractLoggingApiConverter {

    private static final ImmutableSet<String> LOGGING_PACKAGE_PREFIXES = ImmutableSet.of("java.util.logging");

    public JULLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                  Function<String, TargetLogLevel> targetLogLevelFunction,
                                  LogMessageHandler logMessageHandler) {
        super( floggerSuggestedFixGenerator, targetLogLevelFunction, logMessageHandler);
    }

    @Override
    protected Set<String> loggingPackagePrefixes() {
        return LOGGING_PACKAGE_PREFIXES;
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

    protected FloggerLogStatement migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
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

        ImmutableFloggerLogStatement.Builder builder = ImmutableFloggerLogStatement.builder();
        builder.targetLogLevel(targetLogLevel);

        // extract message format argument and it's arguments
        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state);

        // extract throwable argument at the end, if present
        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, state);
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast(remainingArguments);
            builder.thrown(throwableArgument);
        }

        LogMessageModel logMessageModel = createLogMessageModel(messageFormatArgument, remainingArguments,
                state, throwableArgument, migrationContext, targetLogLevel);
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
