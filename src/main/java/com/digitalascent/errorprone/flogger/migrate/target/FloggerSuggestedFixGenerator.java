package com.digitalascent.errorprone.flogger.migrate.target;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.LoggerVariableDefinition;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.common.base.Verify;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

/**
 * Generates code for Flogger logging constructs - loggers & logger invocations
 */
public class FloggerSuggestedFixGenerator {
    private final LoggerVariableDefinition loggerVariableDefinition;

    public FloggerSuggestedFixGenerator(LoggerVariableDefinition loggerVariableDefinition) {
        this.loggerVariableDefinition = requireNonNull(loggerVariableDefinition, "loggerVariableDefinition");
    }

    public SuggestedFix generateConditionalMethod(FloggerConditionalStatement floggerConditionalStatement, MigrationContext migrationContext) {

        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String selectorMethod = generateSelectorMethod(floggerConditionalStatement.targetLogLevel(),
                floggerConditionalStatement.conditionalStatement().state());
        String loggerEnabledMethodCall = String.format("%s.%s.isEnabled()", loggerVariableName, selectorMethod);
        return SuggestedFix.builder()
                .replace(floggerConditionalStatement.conditionalStatement().tree(), loggerEnabledMethodCall)
                .build();
    }

    private String generateSelectorMethod(TargetLogLevel targetLogLevel, VisitorState state) {
        String methodInvocation = targetLogLevel.methodName() + "()";
        if (targetLogLevel.customLogLevel() != null) {
            methodInvocation = String.format("%s(%s)", targetLogLevel.methodName(), state.getSourceForNode(targetLogLevel.customLogLevel()));
        }
        return methodInvocation;
    }

    public SuggestedFix generateLoggingMethod(MethodInvocation loggerMethodInvocation,
                                              FloggerLogStatement floggerLogStatement, MigrationContext migrationContext) {

        String loggerMethodCall = generateLoggingMethodInvocation(floggerLogStatement, loggerMethodInvocation.tree(),
                loggerMethodInvocation.state(), migrationContext);
        SuggestedFix.Builder builder = SuggestedFix.builder()
                .replace(loggerMethodInvocation.tree(), loggerMethodCall);

        // add in any imports the arguments may have added
        addArgumentImports(floggerLogStatement.logMessage(), builder);

        return builder.build();
    }

    private String generateLoggingMethodInvocation(FloggerLogStatement floggerLogStatement, Tree nodeToComment,
                                                   VisitorState visitorState, MigrationContext migrationContext) {
        StringBuilder sb = new StringBuilder(200);

        LogMessage logMessage = floggerLogStatement.logMessage();
        emitComments(nodeToComment, visitorState, logMessage, sb);

        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String selectorMethod = generateSelectorMethod(floggerLogStatement.targetLogLevel(), visitorState);
        String loggingCall = generateLoggingCall(visitorState, floggerLogStatement, loggerVariableName, selectorMethod);
        emitLoggingCall(visitorState, logMessage, loggingCall, sb);
        return sb.toString();
    }

    private String generateLoggingCall(VisitorState state, FloggerLogStatement floggerLogStatement, String loggerVariableName, String methodInvocation) {
        String loggingCall = String.format("%s.%s", loggerVariableName, methodInvocation);

        ExpressionTree thrown = floggerLogStatement.thrown();
        if (thrown != null) {
            String thrownCode = state.getSourceForNode(thrown);
            loggingCall += String.format(".withCause(%s)", thrownCode);
        }
        return loggingCall;
    }

    private void addArgumentImports(LogMessage logMessage, SuggestedFix.Builder builder) {
        logMessage.arguments().stream().map(MessageFormatArgument::imports).flatMap(Collection::stream)
                .forEach(builder::addImport);

        logMessage.arguments().stream().map(MessageFormatArgument::staticImports).flatMap(Collection::stream)
                .forEach(builder::addStaticImport);
    }

    private void emitLoggingCall(VisitorState state, LogMessage logMessage, String loggingCall, StringBuilder sb) {
        sb.append(loggingCall);
        sb.append(".log( ");

        emitMessageFormat(state, logMessage, sb);
        emitMessageFormatArguments(state, logMessage, sb);

        sb.append(" )");
    }

    private void emitComments(Tree tree, VisitorState state, LogMessage logMessage, StringBuilder sb) {
        for (String comment : logMessage.migrationIssues()) {
            sb.append(ToDoCommentGenerator.singleLineCommentForNode(comment, tree, state));
        }
    }

    private void emitMessageFormatArguments(VisitorState state, LogMessage logMessage, StringBuilder sb) {
        for (MessageFormatArgument argument : logMessage.arguments()) {
            sb.append(", ");
            String argumentSrc = argument.code(state);
            sb.append(argumentSrc);
        }
    }

    private void emitMessageFormat(VisitorState state, LogMessage logMessage, StringBuilder sb) {
        if (logMessage.messageFormat() != null) {
            String argumentSrc = "\"" + SourceCodeEscapers.javaCharEscaper().escape(logMessage.messageFormat()) + "\"";
            sb.append(argumentSrc);
        } else {
            if (logMessage.messageFormatArgument() == null) {
                throw new AssertionError("One of messageFormat or messageFormatArgument required");
            }
            sb.append(state.getSourceForNode(logMessage.messageFormatArgument()));
        }
    }

    private String determineLoggerVariableName(MigrationContext migrationContext) {

        Optional<String> existingFloggerLoggerVariable = migrationContext.floggerLoggers().stream()
                .map(x -> x.getName().toString())
                .findFirst();

        if (existingFloggerLoggerVariable.isPresent()) {
            return existingFloggerLoggerVariable.get();
        }

        Optional<String> existingLoggerVariable = migrationContext.classNamedLoggers().stream()
                .map(x -> x.getName().toString())
                .findFirst();

        return existingLoggerVariable.orElseGet(loggerVariableDefinition::name);
    }

    @Nullable
    private Tree findFirstMember(List<? extends Tree> members) {
        return members.stream()
                .filter(x -> !((x instanceof MethodTree) && ASTHelpers.isGeneratedConstructor((MethodTree) x)))
                .findFirst()
                .orElse(null);
    }

    public SuggestedFix processLoggerVariables(ClassTree classTree, VisitorState state, MigrationContext migrationContext) {

        // find the first member in this class such that we can add the logger definition before it
        Tree firstMember = findFirstMember(classTree.getMembers());
        Verify.verify(firstMember != null);

        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String loggerVariable = String.format("%s %s %s %s = %s.%s();", loggerVariableDefinition.scope(), loggerVariableDefinition.modifiers(),
                loggerVariableDefinition.type(), loggerVariableName, loggerVariableDefinition.type(), loggerVariableDefinition.factoryMethod());

        StringBuilder sb = new StringBuilder(200);

        sb.append(loggerVariable);
        sb.append("\n\n");
        sb.append(ASTUtil.determineIndent(firstMember, state));

        SuggestedFix suggestedFix = generateFloggerLogger(firstMember, sb, migrationContext);
        suggestedFix = deleteOriginalLoggers(migrationContext, suggestedFix);

        return suggestedFix;
    }

    private SuggestedFix generateFloggerLogger(Tree firstMember, StringBuilder sb, MigrationContext migrationContext) {
        if (!migrationContext.floggerLoggers().isEmpty()) {
            return SuggestedFix.builder().build();
        }
        return SuggestedFix.builder()
                .prefixWith(firstMember, sb.toString())
                .addImport(loggerVariableDefinition.typeQualified())
                .build();
    }

    private SuggestedFix deleteOriginalLoggers(MigrationContext migrationContext, SuggestedFix suggestedFix) {
        for (VariableTree classNamedLogger : migrationContext.classNamedLoggers()) {
            suggestedFix = SuggestedFix.builder()
                    .delete(classNamedLogger)
                    .merge(suggestedFix)
                    .build();
        }
        return suggestedFix;
    }

    public SuggestedFix removeImport(ImportTree importTree) {
        if (importTree.isStatic()) {
            return SuggestedFix.builder()
                    .removeStaticImport(importTree.getQualifiedIdentifier().toString())
                    .build();
        }
        return SuggestedFix.builder()
                .removeImport(importTree.getQualifiedIdentifier().toString())
                .build();
    }

    public SuggestedFix elideConditional(IfTree ifTree, List<FloggerLogStatement> logStatements, MigrationContext migrationContext, VisitorState state) {

        SuggestedFix.Builder builder = SuggestedFix.builder();

        StringBuilder sb = new StringBuilder(200);

        boolean first = true;
        for (FloggerLogStatement logStatement : logStatements) {
            if (!first) {
                sb.append("\n");
                sb.append(ASTUtil.determineIndent(ifTree, state));
            }
            sb.append(generateLoggingMethodInvocation(logStatement, ifTree, state, migrationContext));
            sb.append(";");
            first = false;

            // add in any imports the arguments may have added
            addArgumentImports(logStatement.logMessage(), builder);
        }

        return builder
                .replace(ifTree, sb.toString())
                .build();
    }
}
