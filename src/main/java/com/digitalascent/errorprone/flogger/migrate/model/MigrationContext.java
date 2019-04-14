package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.sun.source.tree.VariableTree;
import org.immutables.value.Value;

import java.util.List;

@ImmutableStyle
@Value.Immutable
public interface MigrationContext {
    List<VariableTree> classNamedLoggers();
    List<VariableTree> nonClassNamedLoggers();
    List<VariableTree> floggerLoggers();
    String className();
}
