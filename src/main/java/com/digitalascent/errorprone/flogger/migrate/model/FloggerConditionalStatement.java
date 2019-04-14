package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Represents a Flogger conditional statement to determine if logging is enabled or not, for a given log level
 */
@ImmutableStyle
@Value.Immutable
public interface FloggerConditionalStatement {
    TargetLogLevel targetLogLevel();

    MethodInvocation conditionalStatement();
}
