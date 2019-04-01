package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableSuggestionContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.support.ArgumentMatchResult;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.tree.JCTree;

import java.util.Arrays;
import java.util.Optional;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.classType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.logLevelType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.stringType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULMatchers.throwableType;
import static com.digitalascent.errorprone.support.MethodArgumentMatchers.matchArgumentAtIndex;
import static com.digitalascent.errorprone.support.MethodArgumentMatchers.trailingArgument;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * JUL API: https://docs.oracle.com/javase/8/docs/api/java/util/logging/Logger.html
 */
public final class JULLoggingApiConverter implements LoggingApiConverter {
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public JULLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
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

        if (!loggerFactoryMethod().matches(variableTree.getInitializer(), state)) {
            return Optional.empty();
        }

        MethodInvocationTree logManagerMethodInvocationTree = (MethodInvocationTree) variableTree.getInitializer();
        return Optional.of(floggerSuggestedFixGenerator.generateLoggerVariable(classTree, variableTree, state, migrationContext));
    }

    private boolean hasClassParameter(MethodInvocationTree methodInvocationTree, VisitorState state) {
        return classType().matches(methodInvocationTree.getArguments().get(0), state);
    }

    @Override
    public boolean isLoggerVariable(VariableTree tree, VisitorState state) {
        return loggerType().matches(tree, state);
    }

    @Override
    public Optional<SuggestedFix> migrateImport(ImportTree importTree, VisitorState visitorState) {
        if (loggerImports().matches(importTree.getQualifiedIdentifier(), visitorState)) {
            return Optional.of(floggerSuggestedFixGenerator.removeImport(importTree, visitorState));
        }

        return Optional.empty();
    }

    private TargetLogLevel resolveLogLevel(ExpressionTree levelArgument) {
        try {
            if (levelArgument instanceof JCTree.JCFieldAccess) {
                JCTree.JCFieldAccess fieldAccess = (JCTree.JCFieldAccess) levelArgument;
                if (fieldAccess.selected.type.toString().equals("java.util.logging.Level")) {
                    return targetLogLevelFunction.apply(fieldAccess.name.toString());
                }
            }
            return TargetLogLevel.customLogLevel(levelArgument);
        } catch (IllegalArgumentException ignored) {
        }
        throw new SkipCompilationUnitException("Custom log level not supported: " + levelArgument);
    }

    private SuggestedFix migrateConditionalMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                  VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        targetLogLevel = resolveLogLevel(methodInvocationTree.getArguments().get(0));
        return floggerSuggestedFixGenerator.generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
    }

    private SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                              VisitorState state, MigrationContext migrationContext) {
        int messageArgumentIndex = 0;
        int remainingArguments = methodInvocationTree.getArguments().size();

        ImmutableSuggestionContext.Builder builder = ImmutableSuggestionContext.builder();

        TargetLogLevel targetLogLevel;
        if (methodName.equals("log")) {
            ExpressionTree logLevelArgument = methodInvocationTree.getArguments().get(0);
            if (logLevelType().matches(logLevelArgument, state)) {
                targetLogLevel = resolveLogLevel(logLevelArgument);
                builder.addIgnoredArgument(logLevelArgument);
                messageArgumentIndex++;
                remainingArguments--;
            } else {
                return SuggestedFix.builder().build();
            }
        } else {
            targetLogLevel = targetLogLevelFunction.apply(methodName);
        }

        builder.targetLogLevel(targetLogLevel);
        Optional<ArgumentMatchResult> matchResult = trailingArgument(methodInvocationTree, state, throwableType());
        if (matchResult.isPresent()) {
            remainingArguments--;
            builder.thrown(matchResult.get().argument());
        }

        Optional<ArgumentMatchResult> optionalMessageFormatArgumentMatchResult = matchArgumentAtIndex(methodInvocationTree, state, Matchers.anything(), messageArgumentIndex);
        ArgumentMatchResult messageFormatArgumentMatchResult = optionalMessageFormatArgumentMatchResult.orElseThrow(() -> new IllegalArgumentException("Unable to locate message format"));
        ExpressionTree messageFormatArgument = messageFormatArgumentMatchResult.argument();
        builder.messageFormatArgument(messageFormatArgument);
        remainingArguments--;

        if (!stringType().matches(messageFormatArgument, state)) {
            throw new SkipCompilationUnitException("Unable to convert message format: " + messageFormatArgument);
        }

        if (remainingArguments > 0) {
            if (messageFormatArgument instanceof JCTree.JCLiteral) {
                String messageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;
                builder.messageFormatString(convertMessageFormat(messageFormat, remainingArguments));
            } else {
                // if there are arguments to the message format & we were unable to convert the message format
                builder.addComment("Unable to convert message format expression - not a string literal");
            }
        }

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }

    private String convertMessageFormat(String messageFormat, int remainingArguments ) {
        Object[] args = new Object[ remainingArguments ];
        Arrays.fill(args, "%s");
        return java.text.MessageFormat.format( messageFormat, args );
    }
}
