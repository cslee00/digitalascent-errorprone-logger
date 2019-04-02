package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.base.Verify;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;

public class FloggerSuggestedFixGenerator {

    // TODO - configurable
    private static final String FLOGGER_CLASSNAME = "com.google.common.flogger.FluentLogger";
    private final String targetLoggerName = "logger";

    public SuggestedFix generateConditional(MethodInvocationTree tree, VisitorState state, TargetLogLevel targetLogLevel, MigrationContext migrationContext) {
        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String methodInvocation = generateMethodInvocation(targetLogLevel, state);
        return SuggestedFix.builder()
                .replace(tree, String.format("%s.%s.isEnabled()", loggerVariableName, methodInvocation))
                .build();
    }

    private String generateMethodInvocation(TargetLogLevel targetLogLevel, VisitorState state) {
        String methodInvocation = targetLogLevel.methodName() + "()";
        if (targetLogLevel.customLogLevel() != null) {
            methodInvocation = String.format("%s(%s)", targetLogLevel.methodName(), state.getSourceForNode(targetLogLevel.customLogLevel()));
        }
        return methodInvocation;
    }

    @Deprecated
    public SuggestedFix generateLoggingMethod(MethodInvocationTree loggerMethodInvocation, VisitorState state,
                                              ImmutableSuggestionContext suggestionContext, MigrationContext migrationContext) {

        String loggerVariableName = determineLoggerVariableName(migrationContext);

        String methodInvocation = generateMethodInvocation(suggestionContext.targetLogLevel(), state);

        String loggingCall = String.format("%s.%s", loggerVariableName, methodInvocation);

        if (suggestionContext.thrown() != null) {
            String thrownCode = state.getSourceForNode(suggestionContext.thrown());
            loggingCall += String.format(".withCause(%s)", thrownCode);
        }

        StringBuilder sb = new StringBuilder(200);
        if (!suggestionContext.comments().isEmpty()) {
            sb.append("\n");
        }

        for (String comment : suggestionContext.comments()) {
            sb.append("// TODO [LoggerApiRefactoring] ");
            sb.append(comment);
            sb.append("\n");
        }

        sb.append(loggingCall);
        sb.append(".log( ");

        if (suggestionContext.messageFormatString() != null) {
            String argumentSrc = "\"" + SourceCodeEscapers.javaCharEscaper().escape(suggestionContext.messageFormatString()) + "\"";
            if (suggestionContext.forceMissingMessageFormat()) {
                argumentSrc += ", ";
                argumentSrc += state.getSourceForNode(suggestionContext.messageFormatArgument());
            }
            sb.append( argumentSrc );
        } else {
            sb.append( state.getSourceForNode(suggestionContext.messageFormatArgument()));
        }

        boolean firstArgument = true;
        for (ExpressionTree argument : suggestionContext.formatArguments()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String argumentSrc = state.getSourceForNode(argument);
            sb.append(argumentSrc);
        }
        sb.append(" )");

        return SuggestedFix.builder()
                .replace(loggerMethodInvocation, sb.toString())
                .build();
    }

    public SuggestedFix generateLoggingMethod2(MethodInvocationTree loggerMethodInvocation, VisitorState state,
                                              ImmutableFloggerLogContext floggerLogContext, MigrationContext migrationContext) {

        String loggerVariableName = determineLoggerVariableName(migrationContext);

        String methodInvocation = generateMethodInvocation(floggerLogContext.targetLogLevel(), state);

        String loggingCall = String.format("%s.%s", loggerVariableName, methodInvocation);

        if (floggerLogContext.thrown() != null) {
            String thrownCode = state.getSourceForNode(floggerLogContext.thrown());
            loggingCall += String.format(".withCause(%s)", thrownCode);
        }

        StringBuilder sb = new StringBuilder(200);
        if (!floggerLogContext.comments().isEmpty()) {
            sb.append("\n");
        }

        for (String comment : floggerLogContext.comments()) {
            sb.append("// TODO [LoggerApiRefactoring] ");
            sb.append(comment);
            sb.append("\n");
        }

        sb.append(loggingCall);
        sb.append(".log( ");

        if (floggerLogContext.messageFormatString() != null) {
            String argumentSrc = "\"" + SourceCodeEscapers.javaCharEscaper().escape(floggerLogContext.messageFormatString()) + "\"";
            sb.append( argumentSrc );
        } else {
            sb.append( state.getSourceForNode(floggerLogContext.messageFormatArgument()));
        }

        boolean firstArgument = true;
        for (ExpressionTree argument : floggerLogContext.formatArguments()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            String argumentSrc = state.getSourceForNode(argument);
            sb.append(argumentSrc);
        }
        sb.append(" )");

        return SuggestedFix.builder()
                .replace(loggerMethodInvocation, sb.toString())
                .build();
    }

    private String determineLoggerVariableName(MigrationContext migrationContext) {
        return migrationContext.bestLoggerVariableName().orElse(targetLoggerName);
    }

    @Nullable
    private Tree findFirstMember(List<? extends Tree> members) {
        return members.stream().filter(x -> !((x instanceof MethodTree) && ASTHelpers.isGeneratedConstructor((MethodTree) x)))
                .findFirst()
                .orElse(null);
    }

    public SuggestedFix generateLoggerVariable(ClassTree classTree, @Nullable VariableTree variableTree, VisitorState state, MigrationContext migrationContext) {
        // TODO - pull from configuration
        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String loggerVariable = String.format("private static final %s %s = %s.%s();", "FluentLogger", loggerVariableName, "FluentLogger", "forEnclosingClass");

        String code = state.getSourceForNode(classTree);
        Tree firstMember = findFirstMember(classTree.getMembers());
        Verify.verify(firstMember != null);
        SuggestedFix suggestedFix = SuggestedFix.builder()
                .prefixWith(firstMember, loggerVariable + "\n\n")
                .addImport(FLOGGER_CLASSNAME)
                .build();

        if (variableTree != null) {
            suggestedFix = SuggestedFix.builder()
                    .delete(variableTree)
                    .merge(suggestedFix)
                    .build();
        }

        return suggestedFix;
    }

    public SuggestedFix removeImport(ImportTree importTree, VisitorState visitorState) {
        return SuggestedFix.builder()
                .removeImport(importTree.getQualifiedIdentifier().toString())
                .build();
    }
}
