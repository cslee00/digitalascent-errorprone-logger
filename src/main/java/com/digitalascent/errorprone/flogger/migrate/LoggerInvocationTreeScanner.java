package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.BlockTree;
import com.sun.source.tree.ExpressionStatementTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.ParenthesizedTree;
import com.sun.source.tree.StatementTree;
import com.sun.source.tree.Tree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

final class LoggerInvocationTreeScanner extends TreeScanner<Void, VisitorState> {
    private final MigrationContext migrationContext;
    private final List<MethodInvocationTree> loggingMethodInvocations = new ArrayList<>();
    private final List<LoggingConditional> loggingConditionals = new ArrayList<>();
    private final LoggingApiConverter loggingApiConverter;
    private final List<MethodInvocationTree> loggingEnabledMethods = new ArrayList<>();

    LoggerInvocationTreeScanner(MigrationContext migrationContext, LoggingApiConverter loggingApiConverter) {
        this.migrationContext = requireNonNull(migrationContext, "migrationContext");
        this.loggingApiConverter = requireNonNull(loggingApiConverter, "loggingApiConverter");
    }

    List<MethodInvocationTree> loggingMethodInvocations() {
        return loggingMethodInvocations;
    }

    List<LoggingConditional> loggingConditionals() {
        return loggingConditionals;
    }

    List<MethodInvocationTree> loggingEnabledMethods() {
        return loggingEnabledMethods;
    }

    @Override
    public Void scan(Tree tree, VisitorState visitorState) {
        Void retVal = super.scan(tree, visitorState);
        resolveUniqueLoggingMethodInvocations();
        resolveUniqueLoggingEnabledMethodInvocations();
        return retVal;
    }

    private void resolveUniqueLoggingEnabledMethodInvocations() {
        for (LoggingConditional loggingConditional : loggingConditionals) {
            loggingEnabledMethods.remove( loggingConditional.loggingConditionalInvocation() );
        }
    }

    private void resolveUniqueLoggingMethodInvocations() {
        List<MethodInvocationTree> finalList = new ArrayList<>(loggingMethodInvocations);
        loggingMethodInvocations.clear();
        for (LoggingConditional loggingConditional : loggingConditionals) {
            // TODO - type safety
            finalList.removeAll(loggingConditional.loggingMethods());
        }
        loggingMethodInvocations.addAll(finalList);
    }

    @Override
    public Void visitIf(IfTree node, VisitorState visitorState) {
        ParenthesizedTree parenTree = (ParenthesizedTree) node.getCondition();
        ExpressionTree expressionTree = parenTree.getExpression();
        if (loggingApiConverter.matchLoggingEnabledMethod(expressionTree, visitorState)) {
            loggingConditionals.add(createLoggingConditional(node, (MethodInvocationTree) expressionTree, visitorState));
        }
        return super.visitIf(node, visitorState);
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
            } else if (loggingApiConverter.matchLoggingEnabledMethod(node, visitorState)) {
                loggingEnabledMethods.add(node);
            }
        }
        return super.visitMethodInvocation(node, visitorState);
    }

    private boolean isIgnoredLogger(@Nullable String variableName, MigrationContext migrationContext) {
        return migrationContext.nonClassNamedLoggers().stream()
                .anyMatch(loggerVariable -> loggerVariable.getName().toString().equals(variableName));
    }

    private LoggingConditional createLoggingConditional(IfTree ifTree, MethodInvocationTree conditionalExpression, VisitorState state) {
        if (ifTree.getElseStatement() != null) {
            // 'else' on logging conditional isn't idiomatic, unclear what the intent was - don't elide conditional
            return LoggingConditional.migrateExpression(ifTree, conditionalExpression);
        }

        StatementTree statementTree = ifTree.getThenStatement();

        if (statementTree instanceof BlockTree) {
            BlockTree blockTree = (BlockTree) statementTree;
            if (blockTree.getStatements().isEmpty()) {
                // empty conditional - remove it
                return LoggingConditional.elide(ifTree, conditionalExpression);
            }

            // check if all statements are logging statements
            if (blockTree.getStatements().stream()
                    .allMatch(x ->
                            x instanceof ExpressionStatementTree &&
                                    loggingApiConverter.matchLoggingMethod(((ExpressionStatementTree) x).getExpression(), state))) {

                return LoggingConditional.elide(ifTree, conditionalExpression, blockTree.getStatements().stream()
                        .map(x -> ((ExpressionStatementTree) x).getExpression())
                        .map(MethodInvocationTree.class::cast)
                        .collect(toImmutableList()));
            }
        }
        return LoggingConditional.migrateExpression(ifTree, conditionalExpression);
    }
}
