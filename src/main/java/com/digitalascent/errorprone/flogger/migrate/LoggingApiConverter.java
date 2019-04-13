package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggerVariableNamingType;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;

public interface LoggingApiConverter {
    SuggestedFix migrateLoggingMethodInvocation(MethodInvocationTree tree, VisitorState state, MigrationContext migrationContext);
    SuggestedFix migrateLoggingConditional(LoggingConditional loggingConditional, VisitorState state, MigrationContext migrationContext);
    SuggestedFix migrateLoggingEnabledMethod(MethodInvocationTree loggingEnabledMethod, VisitorState state, MigrationContext migrationContext);

    LoggerVariableNamingType determineLoggerVariableNamingType(ClassTree classTree, VariableTree tree, VisitorState state);

    SuggestedFix migrateImport(ImportTree importTree, VisitorState visitorState);

    boolean matchLoggingEnabledMethod(ExpressionTree expressionTree, VisitorState state);

    boolean matchLoggingMethod(ExpressionTree expressionTree, VisitorState state);


}
