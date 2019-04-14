package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggingApiSpecification;
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
    private final List<MethodInvocation> loggingMethodInvocations = new ArrayList<>();
    private final List<LoggingConditional> loggingConditionals = new ArrayList<>();
    private final LoggingApiSpecification loggingApiSpecification;
    private final List<MethodInvocation> loggingEnabledMethods = new ArrayList<>();

    LoggerInvocationTreeScanner(MigrationContext migrationContext, LoggingApiSpecification loggingApiSpecification) {
        this.migrationContext = requireNonNull(migrationContext, "migrationContext");
        this.loggingApiSpecification = requireNonNull(loggingApiSpecification, "loggingApiSpecification");
    }

    List<MethodInvocation> loggingMethodInvocations() {
        return loggingMethodInvocations;
    }

    List<LoggingConditional> loggingConditionals() {
        return loggingConditionals;
    }

    List<MethodInvocation> loggingEnabledMethods() {
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
        List<MethodInvocation> finalList = new ArrayList<>(loggingMethodInvocations);
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
        if (loggingApiSpecification.matchConditionalMethod(expressionTree, visitorState)) {
            loggingConditionals.add(createLoggingConditional(node, MethodInvocation.from((MethodInvocationTree) expressionTree, visitorState)));
        }
        return super.visitIf(node, visitorState);
    }

    @Override
    public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
        if (loggingApiSpecification.matchConditionalMethod(node, visitorState) && loggingApiSpecification.matchLoggingMethod(node, visitorState)) {
            throw new IllegalStateException("Cannot be a logging method and a logging enabled method: " + node);
        }

        String variableName = null;
        Tree methodSelect = node.getMethodSelect();
        if (methodSelect instanceof JCTree.JCFieldAccess) {
            variableName = ((JCTree.JCFieldAccess) methodSelect).selected.toString();
        }

        if (!isIgnoredLogger(variableName, migrationContext)) {
            if (loggingApiSpecification.matchLoggingMethod(node, visitorState)) {
                loggingMethodInvocations.add(MethodInvocation.from(node, visitorState));
            } else if (loggingApiSpecification.matchConditionalMethod(node, visitorState)) {
                loggingEnabledMethods.add(MethodInvocation.from(node, visitorState));
            }
        }
        return super.visitMethodInvocation(node, visitorState);
    }

    private boolean isIgnoredLogger(@Nullable String variableName, MigrationContext migrationContext) {
        return migrationContext.nonClassNamedLoggers().stream()
                .anyMatch(loggerVariable -> loggerVariable.getName().toString().equals(variableName));
    }

    private LoggingConditional createLoggingConditional(IfTree ifTree, MethodInvocation conditionalExpression) {
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
                                    loggingApiSpecification.matchLoggingMethod(((ExpressionStatementTree) x).getExpression(), conditionalExpression.state()))) {

                return LoggingConditional.elide(ifTree, conditionalExpression, blockTree.getStatements().stream()
                        .map(x -> ((ExpressionStatementTree) x).getExpression())
                        .map(MethodInvocationTree.class::cast)
                        .map(x -> MethodInvocation.from(x,conditionalExpression.state()))
                        .collect(toImmutableList()));
            }
        }
        return LoggingConditional.migrateExpression(ifTree, conditionalExpression);
    }
}
