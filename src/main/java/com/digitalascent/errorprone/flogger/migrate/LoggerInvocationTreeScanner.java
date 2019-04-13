package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ConditionalExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

final class LoggerInvocationTreeScanner extends TreeScanner<Void, VisitorState> {
    private final MigrationContext migrationContext;
    private final List<MethodInvocationTree> loggingMethodInvocations = new ArrayList<>();
    private final List<MethodInvocationTree> loggingEnabledMethodInvocations = new ArrayList<>();
    private final List<ConditionalExpressionTree> loggingConditionalExpressions = new ArrayList<>();
    private final LoggingApiConverter loggingApiConverter;

    LoggerInvocationTreeScanner(MigrationContext migrationContext, LoggingApiConverter loggingApiConverter) {
        this.migrationContext = requireNonNull(migrationContext, "migrationContext");
        this.loggingApiConverter = requireNonNull(loggingApiConverter, "loggingApiConverter");
    }

    public List<MethodInvocationTree> loggingMethodInvocations() {
        return loggingMethodInvocations;
    }

    public List<MethodInvocationTree> loggingEnabledMethodInvocations() {
        return loggingEnabledMethodInvocations;
    }

    @Override
    public Void visitConditionalExpression(ConditionalExpressionTree node, VisitorState visitorState) {
        if( loggingApiConverter.matchLoggingEnabledMethod(node.getCondition(), visitorState)) {
            loggingConditionalExpressions.add( node );
        }
        return super.visitConditionalExpression(node, visitorState);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
        if (loggingApiConverter.matchLoggingEnabledMethod(node, visitorState) && loggingApiConverter.matchLoggingMethod(node, visitorState)) {
            throw new IllegalStateException("Cannot be a logging method and a logging enabled method: " + node);
        }

        String variableName = null;
        Tree methodSelect = node.getMethodSelect();
        if (methodSelect instanceof JCTree.JCFieldAccess) {
            variableName = ((JCTree.JCFieldAccess) methodSelect).selected.toString();
        }

        if (!isIgnoredLogger(variableName, migrationContext)) {
            if (loggingApiConverter.matchLoggingMethod(node, visitorState)) {
                loggingMethodInvocations.add(node);
            }
            if (loggingApiConverter.matchLoggingEnabledMethod(node, visitorState)) {
                loggingEnabledMethodInvocations.add(node);
            }
        }
        return super.visitMethodInvocation(node, visitorState);
    }

    private boolean isIgnoredLogger(@Nullable String variableName, MigrationContext migrationContext) {
        return migrationContext.nonClassNamedLoggers().stream()
                .anyMatch(loggerVariable -> loggerVariable.getName().toString().equals(variableName));
    }
}
