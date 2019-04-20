package com.digitalascent.errorprone.flogger;

class LogLevel {
    private final String sourceLogLevel;
    private final String targetLogLevel;
    private boolean lazyArgs;

    LogLevel(String sourceLogLevel, String targetLogLevel, boolean lazyArgs) {
        this.sourceLogLevel = sourceLogLevel;
        this.targetLogLevel = targetLogLevel;
        this.lazyArgs = lazyArgs;
    }

    String sourceLogLevel() {
        return sourceLogLevel;
    }

    String targetLogLevel() {
        return targetLogLevel;
    }

    String sourceLogLevelTitleCase() {
        return Character.toUpperCase(sourceLogLevel().charAt(0)) + sourceLogLevel().substring(1);
    }

    String sourceLogLevelUpperCase() {
        return sourceLogLevel.toUpperCase();
    }

    boolean lazyArgs() {
        return lazyArgs;
    }
}
