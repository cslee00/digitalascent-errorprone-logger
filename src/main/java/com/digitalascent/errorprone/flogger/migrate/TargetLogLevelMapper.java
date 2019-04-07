package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableMap;

import java.util.Map;
import java.util.function.Function;

final class TargetLogLevelMapper implements Function<String, TargetLogLevel> {
    private final ImmutableMap<String, TargetLogLevel> logLevelMap;

    public TargetLogLevelMapper(Map<String, TargetLogLevel> logLevelMap) {
        this.logLevelMap = ImmutableMap.copyOf(logLevelMap);
    }

    @Override
    public TargetLogLevel apply(String level) {
        TargetLogLevel mappedLevel = logLevelMap.get(level.toLowerCase().trim());
        if( mappedLevel == null ) {
            throw new IllegalArgumentException("Unknown log level: " + level);
        }
        return mappedLevel;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("logLevelMap", logLevelMap)
                .toString();
    }
}
