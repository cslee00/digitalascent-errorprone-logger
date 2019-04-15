package com.digitalascent.errorprone.flogger;

class LogLevel {
    private final String sourceLogLevel;
    private final String targetLogLevel;

    LogLevel(String sourceLogLevel, String targetLogLevel) {
        this.sourceLogLevel = sourceLogLevel;
        this.targetLogLevel = targetLogLevel;
    }

    public String sourceLogLevel() {
        return sourceLogLevel;
    }

    public String targetLogLevel() {
        return targetLogLevel;
    }
}
