package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import org.immutables.value.Value;

import javax.annotation.Nullable;

/**
 * Represents a Flogger log statement - the log level, log message, and (optional) thrown exception
 */
@ImmutableStyle
@Value.Immutable
public interface FloggerConditionalStatement {
    TargetLogLevel targetLogLevel();

    MethodInvocationTree conditionalStatement();
}
