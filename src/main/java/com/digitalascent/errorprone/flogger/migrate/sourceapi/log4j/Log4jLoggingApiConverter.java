package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableSuggestionContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.support.MatchResult;
import com.digitalascent.errorprone.support.ExpressionMatchers;
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

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.classType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.logManagerMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.stringType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jMatchers.throwableType;
import static com.digitalascent.errorprone.support.ExpressionMatchers.matchAtIndex;
import static com.digitalascent.errorprone.support.ExpressionMatchers.trailing;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static java.util.Objects.requireNonNull;

/**
 * Log4J API: https://logging.apache.org/log4j/1.2/apidocs/index.html
 */
public final class Log4jLoggingApiConverter implements LoggingApiConverter {
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public Log4jLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
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

        if (!logManagerMethod().matches(variableTree.getInitializer(), state)) {
            return Optional.empty();
        }

        MethodInvocationTree logManagerMethodInvocationTree = (MethodInvocationTree) variableTree.getInitializer();
        // getLogger() or getLogger(getClass())
        if (logManagerMethodInvocationTree.getArguments().size() == 0 || hasClassParameter(logManagerMethodInvocationTree, state)) {
            return Optional.of(floggerSuggestedFixGenerator.generateLoggerVariable(classTree, variableTree, state, migrationContext));
        }

        return Optional.empty();
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
                return targetLogLevelFunction.apply(fieldAccess.name.toString());
            }
        } catch (IllegalArgumentException ignored) {
        }
        throw new SkipCompilationUnitException("Custom log level not supported: " + levelArgument);
    }


    private SuggestedFix migrateConditionalMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                                  VisitorState state, MigrationContext migrationContext) {
        TargetLogLevel targetLogLevel;
        if (methodName.equals("isEnabledFor")) {
            targetLogLevel = resolveLogLevel(methodInvocationTree.getArguments().get(0));
        } else {
            String level = methodName.substring(2).replace("Enabled", "");
            targetLogLevel = targetLogLevelFunction.apply(level);
        }
        return floggerSuggestedFixGenerator.generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
    }

    private SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                              VisitorState state, MigrationContext migrationContext) {
        int messageArgumentIndex = 0;

        ImmutableSuggestionContext.Builder builder = ImmutableSuggestionContext.builder();
        List<? extends ExpressionTree> arguments = methodInvocationTree.getArguments();
        TargetLogLevel targetLogLevel;
        if (methodName.equals("log")) {
            Optional<MatchResult> optionalArgumentMatchResult =
                    ExpressionMatchers.firstMatching(arguments, state, isSubtypeOf("org.apache.log4j.Priority"));
            MatchResult matchResult = optionalArgumentMatchResult.orElseThrow(() -> new IllegalArgumentException("Unable to locate required Priority parameter"));

            targetLogLevel = resolveLogLevel(matchResult.argument());
            messageArgumentIndex = matchResult.index() + 1;
        } else {
            targetLogLevel = targetLogLevelFunction.apply(methodName);
        }

        builder.targetLogLevel(targetLogLevel);
        Optional<MatchResult> matchResult = trailing(arguments, state, throwableType());
        matchResult.ifPresent(thrownMatchResult -> builder.thrown(thrownMatchResult.argument()));

        Optional<MatchResult> optionalMessageFormatArgumentMatchResult = matchAtIndex(arguments, state, Matchers.anything(), messageArgumentIndex);
        MatchResult messageFormatMatchResult = optionalMessageFormatArgumentMatchResult.orElseThrow(
                () -> new IllegalArgumentException("Unable to locate message format"));
        ExpressionTree messageFormatArgument = messageFormatMatchResult.argument();
        builder.messageFormatArgument(messageFormatArgument);

        if (!stringType().matches(messageFormatArgument, state)) {
            builder.messageFormatString("%s");
            builder.forceMissingMessageFormat(true);
        }

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }
}
