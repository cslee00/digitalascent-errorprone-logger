package com.digitalascent.errorprone.flogger.migrate.target;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.LoggerVariableDefinition;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.common.base.Verify;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.google.errorprone.util.SourceCodeEscapers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.MethodTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static java.util.Objects.requireNonNull;

public class FloggerSuggestedFixGenerator {
    private final LoggerVariableDefinition loggerVariableDefinition;

    public FloggerSuggestedFixGenerator(LoggerVariableDefinition loggerVariableDefinition) {
        this.loggerVariableDefinition = requireNonNull(loggerVariableDefinition, "loggerVariableDefinition");
    }

    public SuggestedFix generateConditional(MethodInvocationTree tree, VisitorState state, TargetLogLevel targetLogLevel, MigrationContext migrationContext) {

        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String selectorMethod = generateSelectorMethod(targetLogLevel, state);
        return SuggestedFix.builder()
                .replace(tree, String.format("%s.%s.isEnabled()", loggerVariableName, selectorMethod))
                .build();
    }

    private String generateSelectorMethod(TargetLogLevel targetLogLevel, VisitorState state) {
        String methodInvocation = targetLogLevel.methodName() + "()";
        if (targetLogLevel.customLogLevel() != null) {
            methodInvocation = String.format("%s(%s)", targetLogLevel.methodName(), state.getSourceForNode(targetLogLevel.customLogLevel()));
        }
        return methodInvocation;
    }

    public SuggestedFix generateLoggingMethod(MethodInvocationTree loggerMethodInvocation, VisitorState state,
                                              FloggerLogStatement floggerLogStatement, MigrationContext migrationContext) {

        StringBuilder sb = new StringBuilder(200);

        LogMessageModel logMessageModel = floggerLogStatement.logMessageModel();
        emitComments(loggerMethodInvocation, state, logMessageModel, sb);

        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String selectorMethod = generateSelectorMethod(floggerLogStatement.targetLogLevel(), state);
        String loggingCall = generateLoggingCall(state, floggerLogStatement, loggerVariableName, selectorMethod);
        emitLoggingCall(state, logMessageModel, loggingCall, sb);

        SuggestedFix.Builder builder = SuggestedFix.builder()
                .replace(loggerMethodInvocation, sb.toString());

        // add in any imports the arguments may have added
        addArgumentImports(logMessageModel, builder);

        return builder.build();
    }

    private String generateLoggingCall(VisitorState state, FloggerLogStatement floggerLogStatement, String loggerVariableName, String methodInvocation) {
        String loggingCall = String.format("%s.%s", loggerVariableName, methodInvocation);

        if (floggerLogStatement.thrown() != null) {
            String thrownCode = state.getSourceForNode(floggerLogStatement.thrown());
            loggingCall += String.format(".withCause(%s)", thrownCode);
        }
        return loggingCall;
    }

    private void addArgumentImports(LogMessageModel logMessageModel, SuggestedFix.Builder builder) {
        logMessageModel.arguments().stream().map(MessageFormatArgument::imports).flatMap(Collection::stream)
                .forEach(builder::addImport);

        logMessageModel.arguments().stream().map(MessageFormatArgument::staticImports).flatMap(Collection::stream)
                .forEach(builder::addStaticImport);
    }

    private void emitLoggingCall(VisitorState state, LogMessageModel logMessageModel, String loggingCall, StringBuilder sb) {
        sb.append(loggingCall);
        sb.append(".log( ");

        emitMessageFormat(state, logMessageModel, sb);
        emitMessageFormatArguments(state, logMessageModel, sb);

        sb.append(" )");
    }

    private void emitComments(MethodInvocationTree loggerMethodInvocation, VisitorState state, LogMessageModel logMessageModel, StringBuilder sb) {
        for (String comment : logMessageModel.migrationIssues()) {
            sb.append(ToDoCommentGenerator.singleLineCommentForNode(comment, loggerMethodInvocation, state));
        }
    }

    private void emitMessageFormatArguments(VisitorState state, LogMessageModel logMessageModel, StringBuilder sb) {
        for (MessageFormatArgument argument : logMessageModel.arguments()) {
            sb.append(", ");
            String argumentSrc = argument.code(state);
            sb.append(argumentSrc);
        }
    }

    private void emitMessageFormat(VisitorState state, LogMessageModel logMessageModel, StringBuilder sb) {
        if (logMessageModel.messageFormat() != null) {
            String argumentSrc = "\"" + SourceCodeEscapers.javaCharEscaper().escape(logMessageModel.messageFormat()) + "\"";
            sb.append(argumentSrc);
        } else {
            if (logMessageModel.messageFormatArgument() == null) {
                throw new AssertionError("One of messageFormat or messageFormatArgument required");
            }
            sb.append(state.getSourceForNode(logMessageModel.messageFormatArgument()));
        }
    }

    private String determineLoggerVariableName(MigrationContext migrationContext) {

        Optional<String> existingFloggerLoggerVariable = migrationContext.floggerLoggers().stream()
                .map( x -> x.getName().toString() )
                .findFirst();

        if( existingFloggerLoggerVariable.isPresent() ) {
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
        if( !migrationContext.floggerLoggers().isEmpty()) {
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
}
