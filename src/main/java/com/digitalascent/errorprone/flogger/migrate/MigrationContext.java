package com.digitalascent.errorprone.flogger.migrate;

import com.sun.source.tree.VariableTree;

import java.util.List;
import java.util.Optional;

public interface MigrationContext {
    List<VariableTree> sourceLoggerMemberVariables();
    List<VariableTree> floggerMemberVariables();
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
}
