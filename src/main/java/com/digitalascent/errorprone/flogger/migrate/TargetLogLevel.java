package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.base.MoreObjects;
import com.sun.source.tree.ExpressionTree;

public final class TargetLogLevel {
    private final String methodName;
    private final ExpressionTree customLogLevel;

    public static TargetLogLevel customLogLevel( ExpressionTree customLogLevel ) {
        return new TargetLogLevel("at", customLogLevel );
    }
    public TargetLogLevel(String methodName) {
        this(methodName, null);
    }

    public TargetLogLevel(String methodName, ExpressionTree customLogLevel) {
        this.methodName = methodName;
        this.customLogLevel = customLogLevel;
    }

    public String methodName() {
        return methodName;
    }

    ExpressionTree customLogLevel() {
        return customLogLevel;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("methodName", methodName)
                .add("customLogLevel", customLogLevel)
                .toString();
    }
}
