package com.digitalascent.errorprone.flogger.migrate;

import java.util.function.Function;

// TODO - make configurable
class TargetLogLevelMapper implements Function<String, TargetLogLevel> {
    @Override
    public TargetLogLevel apply(String level) {
        switch (level.toLowerCase().trim()) {
            case "trace":
            case "finest":
            case "finer":
                return new TargetLogLevel("atFinest");
            case "debug":
            case "fine":
            case "config":
                return new TargetLogLevel("atFine");
            case "info":
                return new TargetLogLevel("atInfo");
            case "warn":
            case "warning":
                return new TargetLogLevel("atWarning");
            case "error":
            case "fatal":
            case "severe":
                return new TargetLogLevel("atSevere");
        }
        throw new IllegalArgumentException("Unknown log level: " + level);
    }
}
