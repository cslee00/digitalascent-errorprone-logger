package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggerVariableNamingType;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;

import java.util.Optional;

public interface LoggingApiConverter {
    Optional<SuggestedFix> migrateLoggingMethodInvocation(MethodInvocationTree tree, VisitorState state, MigrationContext migrationContext);

    LoggerVariableNamingType determineLoggerVariableNamingType(ClassTree classTree, VariableTree tree, VisitorState state);

    Optional<SuggestedFix> migrateImport(ImportTree importTree, VisitorState visitorState);
}
