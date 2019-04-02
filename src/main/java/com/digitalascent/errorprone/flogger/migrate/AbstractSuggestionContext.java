package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Set;

@ImmutableStyle
@Value.Immutable
public abstract class AbstractSuggestionContext {

    public abstract TargetLogLevel targetLogLevel();

    @Value.Default
    public List<? extends ExpressionTree> formatArguments() {
        return ImmutableList.of();
    }

    @Nullable
    public abstract ExpressionTree messageFormatArgument();

    @Value.Default
    public boolean forceMissingMessageFormat() {
        return false;
    }

    @Nullable
    public abstract String messageFormatString();

    @Nullable
    public abstract ExpressionTree thrown();

    public abstract List<String> comments();
}
