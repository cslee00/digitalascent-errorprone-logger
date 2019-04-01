package com.digitalascent.errorprone.flogger.migrate;

import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.Optional;

public interface LoggingApiConverter {
    Optional<SuggestedFix> migrateLoggingMethodInvocation(MethodInvocationTree tree, VisitorState state, MigrationContext migrationContext);

    Optional<SuggestedFix> migrateLoggerVariable(ClassTree classTree, @Nullable VariableTree tree, VisitorState state, MigrationContext migrationContext);

    boolean isLoggerVariable(VariableTree tree, VisitorState state);

    Optional<SuggestedFix> migrateImport(ImportTree importTree, VisitorState visitorState);
}
