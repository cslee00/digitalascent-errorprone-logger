package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.sun.source.tree.ExpressionTree;
import org.immutables.value.Value;

import javax.annotation.Nullable;

@ImmutableStyle
@Value.Immutable
public abstract class AbstractFloggerLogContext {

    public abstract LogMessageModel logMessageModel();
    public abstract TargetLogLevel targetLogLevel();

    @Nullable
    public abstract ExpressionTree thrown();

}
