package com.digitalascent.errorprone.flogger.migrate;

import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface MigrationContext {
    List<VariableTree> classNamedLoggers();
    List<VariableTree> nonClassNamedLoggers();
    List<VariableTree> floggerLoggers();
    Optional<String> floggerLoggerVariableName();
}
