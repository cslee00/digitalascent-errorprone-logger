package com.digitalascent.errorprone.flogger.migrate;

import com.sun.source.tree.VariableTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

public interface MigrationContext {
    List<VariableTree> classNamedLoggers();
    List<VariableTree> nonClassNamedLoggers();
    List<VariableTree> floggerLoggers();
    Optional<String> floggerMemberVariableName();
    Optional<String> sourceLoggerMemberVariableName();

    default Optional<String> bestLoggerVariableName() {
        if( floggerMemberVariableName().isPresent() ) {
            // use existing Flogger instance, if available
            return floggerMemberVariableName();
        }

        if( sourceLoggerMemberVariableName().isPresent()) {
            // use existing logger variable name, if available
            return sourceLoggerMemberVariableName();
        }

        return Optional.empty();
    }

    default boolean isIgnoredLogger(@Nullable  String variableName) {
        for (VariableTree logger : nonClassNamedLoggers()) {
            if( logger.getName().toString().equals( variableName ) ) {
                return true;
            }
        }
        return false;
    }
}
