package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import org.immutables.value.Value;

/**
 * Represents a Flogger conditional statement to determine if logging is enabled or not, for a given log level
 */
@ImmutableStyle
@Value.Immutable
public interface FloggerConditionalStatement {
    TargetLogLevel targetLogLevel();

    MethodInvocation conditionalStatement();
}
