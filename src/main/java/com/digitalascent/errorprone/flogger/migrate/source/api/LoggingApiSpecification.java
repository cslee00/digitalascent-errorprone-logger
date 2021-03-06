package com.digitalascent.errorprone.flogger.migrate.source.api;

import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.Set;

public interface LoggingApiSpecification {

    boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState);

    boolean matchConditionalMethod(ExpressionTree expressionTree, VisitorState state);

    boolean matchLoggingMethod(ExpressionTree expressionTree, VisitorState state);

    boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState);

    FloggerConditionalStatement parseConditionalMethod(MethodInvocation methodInvocation);

    FloggerLogStatement parseLoggingMethod(MethodInvocation loggingMethodInvocation, MigrationContext migrationContext);

    boolean shouldRemoveImport(String importString);
}
