package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2;

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

import java.util.Optional;
import java.util.function.Function;


import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.logType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.stringType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.throwableType;
import static com.digitalascent.errorprone.support.MethodArgumentMatchers.matchArgumentAtIndex;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Tiny Log 2 API: https://tinylog.org/v2/javadoc/
 */
public final class TinyLog2LoggingApiConverter implements LoggingApiConverter {
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public TinyLog2LoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
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

        return Optional.empty();
    }

    @Override
    public Optional<SuggestedFix> migrateLoggerVariable(ClassTree classTree, VariableTree variableTree,
                                                        VisitorState state, MigrationContext migrationContext) {
        checkArgument(isLoggerVariable(variableTree, state), "isLoggerVariable(variableTree, state) : %s", variableTree);

        // NO-OP - TinyLog is entirely static

        return Optional.empty();
    }

    @Override
    public boolean isLoggerVariable(VariableTree tree, VisitorState state) {
        return logType().matches(tree, state);
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
                return targetLogLevelFunction.apply(fieldAccess.name.toString());
            }
        } catch (IllegalArgumentException ignored) {
        }
        throw new SkipCompilationUnitException("Custom log level not supported: " + levelArgument);
    }

    private SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                              VisitorState state, MigrationContext migrationContext) {
        int remainingArguments = methodInvocationTree.getArguments().size();

        ImmutableSuggestionContext.Builder builder = ImmutableSuggestionContext.builder();

        TargetLogLevel targetLogLevel;
        targetLogLevel = targetLogLevelFunction.apply(methodName);

        builder.targetLogLevel(targetLogLevel);
        Optional<ArgumentMatchResult> optionalThrownMatchResult = matchArgumentAtIndex(methodInvocationTree, state, throwableType(), 0);
        if( optionalThrownMatchResult.isPresent() ) {
            remainingArguments--;
            optionalThrownMatchResult.ifPresent(thrownMatchResult -> builder.thrown(thrownMatchResult.argument()));
        }

        Optional<ArgumentMatchResult> optionalMessageFormatArgumentMatchResult =
                matchArgumentAtIndex(methodInvocationTree, state, Matchers.anything(),
                        optionalThrownMatchResult.isPresent() ? 1 : 0);
        if( optionalMessageFormatArgumentMatchResult.isPresent() ) {
            remainingArguments--;
            ArgumentMatchResult matchResult = optionalMessageFormatArgumentMatchResult.get();
            ExpressionTree messageFormatArgument = matchResult.argument();
            builder.messageFormatArgument(messageFormatArgument);

            if (!stringType().matches(messageFormatArgument, state)) {
                builder.messageFormatString("%s");
                builder.forceMissingMessageFormat(true);
            } else {
                if (messageFormatArgument instanceof JCTree.JCLiteral ) {
                    String messageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;
                    builder.messageFormatString(TinyLog2MessageFormatter.format(messageFormat));
                } else {
                    // if there are arguments to the message format & we were unable to convert the message format
                    if (remainingArguments > 0) {
                        builder.addComment("Unable to convert message format expression - not a string literal");
                    }
                }
            }
        }

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }
}