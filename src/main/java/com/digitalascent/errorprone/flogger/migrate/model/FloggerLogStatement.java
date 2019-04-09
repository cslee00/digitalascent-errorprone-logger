package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.sun.source.tree.ExpressionTree;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ImmutableStyle
@Value.Immutable
public interface FloggerLogStatement {
    LogMessageModel logMessageModel();

    TargetLogLevel targetLogLevel();

    @Nullable
    ExpressionTree thrown();
}
