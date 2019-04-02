package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipCompilationUnitException;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
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

import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.classType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.logManagerMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.loggerImports;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.loggerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.loggingEnabledMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.loggingMethod;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.markerType;
import static com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2Matchers.stringType;
import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

/**
 * Log4J2 API: https://logging.apache.org/log4j/2.x/log4j-api/apidocs/index.html
 */
public final class Log4j2LoggingApiConverter implements LoggingApiConverter {
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public Log4j2LoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
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
        if (methodName.equals("isEnabled")) {
            targetLogLevel = resolveLogLevel(methodInvocationTree.getArguments().get(0));
        } else {
            String level = methodName.substring(2).replace("Enabled", "");
            targetLogLevel = targetLogLevelFunction.apply(level);
        }
        return floggerSuggestedFixGenerator.generateConditional(methodInvocationTree, state, targetLogLevel, migrationContext);
    }

    private SuggestedFix migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree,
                                              VisitorState state, MigrationContext migrationContext) {

        List<? extends ExpressionTree> remainingArguments = methodInvocationTree.getArguments();
        TargetLogLevel targetLogLevel;
        if (methodName.equals("log")) {
            ExpressionTree logLevelArgument = remainingArguments.get(0);
            targetLogLevel = resolveLogLevel(logLevelArgument);
            remainingArguments = Arguments.removeFirst(remainingArguments);
        } else {
            targetLogLevel = targetLogLevelFunction.apply(methodName);
        }

        ImmutableFloggerLogContext.Builder builder = ImmutableFloggerLogContext.builder();
        builder.targetLogLevel(targetLogLevel);

        if( hasMarkerArgument(remainingArguments,state)) {
            remainingArguments = Arguments.removeFirst(remainingArguments);
        }

        ExpressionTree messageFormatArgument = findMessageFormatArgument(remainingArguments, state);
        builder.messageFormatArgument(messageFormatArgument);
        remainingArguments = Arguments.findMessageFormatArguments(remainingArguments, state);

        ExpressionTree throwableArgument = Arguments.findTrailingThrowable(remainingArguments, state);
        if (throwableArgument != null) {
            remainingArguments = Arguments.removeLast(remainingArguments);
            builder.thrown(throwableArgument);
        }

        String messageFormatString = null;
        if (isMessageFormatObject(state, messageFormatArgument)) {
            // object as format string - push it as an argument
            remainingArguments = Arguments.prependArgument(remainingArguments, messageFormatArgument);
            messageFormatString = "%s";
        } else {
            if (!stringType().matches(messageFormatArgument, state)) {
                throw new SkipLogMethodException("Unable to convert message format: " + messageFormatArgument);
            }
        }

        if (!remainingArguments.isEmpty() && messageFormatString == null) {
            if ( messageFormatArgument instanceof JCTree.JCLiteral) {
                String messageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;
                messageFormatString = convertMessageFormat(messageFormat, migrationContext, builder);
            } else {
                // if there are arguments to the message format & we were unable to convert the message format
                builder.addComment("Unable to convert message format expression - not a string literal");
            }
        }
        builder.formatArguments(remainingArguments);
        builder.messageFormatString(messageFormatString);

        return floggerSuggestedFixGenerator.generateLoggingMethod(methodInvocationTree, state, builder.build(), migrationContext);
    }

    private boolean isMessageFormatObject(VisitorState state, ExpressionTree messageFormatArgument) {
        return Matchers.isSameType("java.lang.Object").matches(messageFormatArgument, state);
    }

    private boolean hasMarkerArgument(List<? extends ExpressionTree> arguments, VisitorState state) {
        if( arguments.isEmpty() ) {
            return false;
        }
        return markerType().matches(arguments.get(0),state);
    }

    private ExpressionTree findMessageFormatArgument(List<? extends ExpressionTree> arguments,  VisitorState state) {
        if( arguments.isEmpty() ) {
            throw new IllegalStateException("Unable to find required message format argument");
        }
        return arguments.get(0);
    }

    private String convertMessageFormat(String messageFormat, MigrationContext migrationContext, ImmutableFloggerLogContext.Builder builder) {
        if (migrationContext.sourceLoggerMemberVariables().size() != 1) {
            // no existing variable definition (likely from a superclass or elsewhere)
            // assume default
            builder.addComment("Unable to determine parameter format type; assuming default (brace style)");
            return Log4j2BraceMessageFormatConverter.convertMessageFormat(messageFormat);
        }
        MethodInvocationTree logFactoryMethodInvocationTree = (MethodInvocationTree) migrationContext.sourceLoggerMemberVariables().get(0).getInitializer();
        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(logFactoryMethodInvocationTree);
        String methodName = sym.getSimpleName().toString();
        if ("getLogger".equals(methodName)) {
            return Log4j2BraceMessageFormatConverter.convertMessageFormat(messageFormat);
        }
        return messageFormat;
    }
}
