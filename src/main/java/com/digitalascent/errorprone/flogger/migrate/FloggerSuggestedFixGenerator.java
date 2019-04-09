package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
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
    private final LoggerDefinition loggerDefinition;

    public FloggerSuggestedFixGenerator(LoggerDefinition loggerDefinition) {
        this.loggerDefinition = requireNonNull(loggerDefinition, "loggerDefinition");
    }

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

        emitMessageFormat(state, logMessageModel, sb);
        emitMessageFormatArguments(state, logMessageModel, sb);

        sb.append(" )");

        SuggestedFix.Builder builder = SuggestedFix.builder()
                .replace(loggerMethodInvocation, sb.toString());

        // add in any imports the arguments may have added
        logMessageModel.arguments().stream().map(MessageFormatArgument::imports).flatMap(Collection::stream)
                .forEach(builder::addImport);

        logMessageModel.arguments().stream().map(MessageFormatArgument::staticImports).flatMap(Collection::stream)
                .forEach(builder::addStaticImport);

        return builder.build();
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

        return existingLoggerVariable.orElseGet(loggerDefinition::name);
    }

    @Nullable
    private Tree findFirstMember(List<? extends Tree> members) {
        return members.stream()
                .filter(x -> !((x instanceof MethodTree) && ASTHelpers.isGeneratedConstructor((MethodTree) x)))
                .findFirst()
                .orElse(null);
    }

    SuggestedFix processLoggerVariables(ClassTree classTree, VisitorState state, MigrationContext migrationContext) {

        // find the first member in this class such that we can add the logger definition before it
        Tree firstMember = findFirstMember(classTree.getMembers());
        Verify.verify(firstMember != null);

        String loggerVariableName = determineLoggerVariableName(migrationContext);
        String loggerVariableDefinition = String.format("%s %s %s %s = %s.%s();", loggerDefinition.scope(), loggerDefinition.modifiers(),
                loggerDefinition.type(), loggerVariableName, loggerDefinition.type(), loggerDefinition.factoryMethod());

        StringBuilder sb = new StringBuilder(200);

        sb.append(loggerVariableDefinition);
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
                    .addImport(loggerDefinition.typeQualified())
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
