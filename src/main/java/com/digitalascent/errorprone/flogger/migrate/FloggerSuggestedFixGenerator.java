package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
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
        String selectorMethod = generateSelector(targetLogLevel, state);
        return SuggestedFix.builder()
                .replace(tree, String.format("%s.%s.isEnabled()", loggerVariableName, selectorMethod))
                .build();
    }

    private String generateSelector(TargetLogLevel targetLogLevel, VisitorState state) {
        String methodInvocation = targetLogLevel.methodName() + "()";
        if (targetLogLevel.customLogLevel() != null) {
            methodInvocation = String.format("%s(%s)", targetLogLevel.methodName(), state.getSourceForNode(targetLogLevel.customLogLevel()));
        }
        return methodInvocation;
    }

    public SuggestedFix generateLoggingMethod(MethodInvocationTree loggerMethodInvocation, VisitorState state,
                                              ImmutableFloggerLogContext floggerLogContext, MigrationContext migrationContext) {

        LogMessageModel logMessageModel = floggerLogContext.logMessageModel();

        String loggerVariableName = determineLoggerVariableName(migrationContext);

        String methodInvocation = generateSelector(floggerLogContext.targetLogLevel(), state);

        String loggingCall = String.format("%s.%s", loggerVariableName, methodInvocation);

        if (floggerLogContext.thrown() != null) {
            String thrownCode = state.getSourceForNode(floggerLogContext.thrown());
            loggingCall += String.format(".withCause(%s)", thrownCode);
        }

        StringBuilder sb = new StringBuilder(200);

        for (String comment : logMessageModel.migrationIssues()) {
            sb.append(ToDoCommentGenerator.singleLineCommentForNode(comment, loggerMethodInvocation, state));
        }

        sb.append(loggingCall);
        sb.append(".log( ");

        if (logMessageModel.messageFormat() != null) {
            String argumentSrc = "\"" + SourceCodeEscapers.javaCharEscaper().escape(logMessageModel.messageFormat()) + "\"";
            sb.append(argumentSrc);
        } else {
            sb.append(state.getSourceForNode(logMessageModel.messageFormatArgument()));
        }

        for (ExpressionTree argument : logMessageModel.arguments()) {
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
        Tree firstMember = findFirstMember(classTree.getMembers());
        Verify.verify(firstMember != null);

        StringBuilder stringBuilder = new StringBuilder(200);
        String loggerVariableName = determineLoggerVariableName(migrationContext);
        stringBuilder.append(String.format("private static final %s %s = %s.%s();", "FluentLogger", loggerVariableName, "FluentLogger", "forEnclosingClass"));
        stringBuilder.append("\n\n");
        stringBuilder.append(ASTUtil.determineIndent(firstMember, state));

        SuggestedFix suggestedFix = SuggestedFix.builder()
                .prefixWith(firstMember, stringBuilder.toString())
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

    public SuggestedFix removeImport(ImportTree importTree) {
        return SuggestedFix.builder()
                .removeImport(importTree.getQualifiedIdentifier().toString())
                .build();
    }

}
