package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableSuggestionContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
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

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.classType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerFactoryMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.markerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.stringType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jMatchers.throwableType;
import static com.digitalascent.errorprone.support.MethodArgumentMatchers.firstMatchingArgument;
import static com.digitalascent.errorprone.support.MethodArgumentMatchers.matchArgumentAtIndex;
import static com.digitalascent.errorprone.support.MethodArgumentMatchers.trailingArgument;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * SLF4J API: https://www.slf4j.org/apidocs/index.html
 */
public final class Slf4JLoggingApiConverter implements LoggingApiConverter {
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public Slf4JLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
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
    public Optional<SuggestedFix> migrateLoggerVariable(ClassTree classTree, VariableTree variableTree, VisitorState state, MigrationContext migrationContext) {
        checkArgument(isLoggerVariable(variableTree, state), "isLoggerVariable(variableTree, state) : %s", variableTree);

        if (!loggerFactoryMethod().matches(variableTree.getInitializer(), state)) {
            return Optional.empty();
        }

        // the call to LoggerFactory.getMethod; if it isn't a class parameter then we can't migrate it
        if (!hasClassParameter((MethodInvocationTree) variableTree.getInitializer(), state)) {
            return Optional.empty();
        }

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

    private SuggestedFix migrateConditionalMethod(TargetLogLevel targetLogLevel, MethodInvocationTree tree, VisitorState state, MigrationContext migrationContext) {
        return floggerSuggestedFixGenerator.generateConditional(tree, state, targetLogLevel, migrationContext);
    }

    private SuggestedFix migrateLoggingMethod(TargetLogLevel targetLogLevel, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {
        // always have message format
        int messageFormatIndex = 0;
        int remainingArguments = methodInvocationTree.getArguments().size();

        ImmutableSuggestionContext.Builder builder = ImmutableSuggestionContext.builder();
        builder.targetLogLevel(targetLogLevel);
        Optional<ArgumentMatchResult> optionalThrowableMatchResult = trailingArgument(methodInvocationTree, state, throwableType());
        if (optionalThrowableMatchResult.isPresent()) {
            remainingArguments--;
            builder.thrown(optionalThrowableMatchResult.get().argument());
        }

        Optional<ArgumentMatchResult> optionalMarkerMatchResult = firstMatchingArgument(methodInvocationTree, state, markerType());
        if (optionalMarkerMatchResult.isPresent()) {
            ArgumentMatchResult result = optionalMarkerMatchResult.get();
            // we're looking for the Marker that may be the firstMatchingArgument parameter
            if (result.index() == 0) {
                remainingArguments--;
                builder.addIgnoredArgument(result.argument());
                messageFormatIndex++;
            }
        }

        Optional<ArgumentMatchResult> optionalMessageFormatMatchResult = matchArgumentAtIndex(methodInvocationTree, state, Matchers.anything(), messageFormatIndex);
        ArgumentMatchResult messageFormatArgumentMatchResult = optionalMessageFormatMatchResult.orElseThrow(() -> new IllegalStateException("Missing message format parameter"));
        ExpressionTree messageFormatArgument = messageFormatArgumentMatchResult.argument();
        builder.messageFormatArgument(messageFormatArgument);
        remainingArguments--;
        if (remainingArguments > 0) {
            if (messageFormatArgument instanceof JCTree.JCLiteral && stringType().matches(messageFormatArgument, state)) {
                String messageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;
                builder.messageFormatString(Slf4jMessageFormatConverter.convertMessageFormat(messageFormat));
            } else {
                // if there are arguments to the message format & we were unable to convert the message format
                builder.addComment("Unable to convert message format expression - not a string literal");
            }
        }

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }
}
