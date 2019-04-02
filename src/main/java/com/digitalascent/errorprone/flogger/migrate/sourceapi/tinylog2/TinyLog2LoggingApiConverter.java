package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.support.MatchResult;
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

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.logType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2Matchers.loggingMethod;
import static com.digitalascent.errorprone.support.ExpressionMatchers.matchAtIndex;
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
        ImmutableFloggerLogContext.Builder builder = ImmutableFloggerLogContext.builder();

        TargetLogLevel targetLogLevel;
        targetLogLevel = targetLogLevelFunction.apply(methodName);
        builder.targetLogLevel(targetLogLevel);

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();
        ExpressionTree throwableArgument = findThrowableArgument(remainingArguments,state);
        if( throwableArgument != null ) {
            builder.thrown(throwableArgument);
            remainingArguments = Arguments.removeFirst( remainingArguments );
        }

        String messageFormat = null;
        if( remainingArguments.isEmpty() ) {
            if( throwableArgument != null ) {
                messageFormat = "Exception";
            }
        } else {
            ExpressionTree argument = remainingArguments.get(0);
            if( Matchers.isSameType("java.lang.Object").matches(argument,state)) {
                messageFormat = "%s";
            } else {
                if (!TinyLog2Matchers.stringType().matches(argument, state)) {
                    throw new SkipCompilationUnitException("Unable to handle " + argument);
                }
                builder.messageFormatArgument( argument );
                remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state );

                if (!remainingArguments.isEmpty()) {
                    if (argument instanceof JCTree.JCLiteral) {
                        String messageFormatStr = (String) ((JCTree.JCLiteral) argument).value;
                        messageFormat = TinyLog2MessageFormatter.format(messageFormatStr);
                    } else {
                        // if there are arguments to the message format & we were unable to convert the message format
                        builder.addComment("Unable to convert message format expression - not a string literal");
                    }
                }
            }
        }
        builder.messageFormatString(messageFormat);
        builder.formatArguments(remainingArguments);

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }

    @Nullable
    private ExpressionTree findThrowableArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        Optional<MatchResult> optionalThrownMatchResult = matchAtIndex(arguments, state, TinyLog2Matchers.throwableType(), 0);
        return optionalThrownMatchResult.map(MatchResult::argument).orElse(null);
    }
}