package com.digitalascent.errorprone.flogger.migrate;

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

    public ExpressionTree customLogLevel() {
        return customLogLevel;
    }
}
