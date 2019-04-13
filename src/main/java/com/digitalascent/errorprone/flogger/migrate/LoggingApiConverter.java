package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggerVariableNamingType;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;

public interface LoggingApiConverter {
    SuggestedFix migrateLoggingMethodInvocation(MethodInvocationTree tree, VisitorState state, MigrationContext migrationContext);
    SuggestedFix migrateLoggingEnabledMethodInvocation(MethodInvocationTree loggingEnabledMethodInvocation, VisitorState state, MigrationContext migrationContext);

    LoggerVariableNamingType determineLoggerVariableNamingType(ClassTree classTree, VariableTree tree, VisitorState state);

    SuggestedFix migrateImport(ImportTree importTree, VisitorState visitorState);

    boolean matchLoggingEnabledMethod(MethodInvocationTree methodInvocationTree, VisitorState state);

    boolean matchLoggingMethod(MethodInvocationTree methodInvocationTree, VisitorState state);
}
