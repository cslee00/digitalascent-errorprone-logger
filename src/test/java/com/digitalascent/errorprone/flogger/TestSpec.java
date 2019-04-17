package com.digitalascent.errorprone.flogger;

import com.google.common.base.MoreObjects;

final class TestSpec {
    private final String testSource;
    private final String expectedSource;
    private final LogLevel logLevel;
    private final String name;

    TestSpec(LogLevel logLevel, String name, String testSource, String expectedSource) {
        this.logLevel = logLevel;
        this.name = name;
        this.testSource = testSource;
        this.expectedSource = expectedSource;
    }

    String testSource() {
        return testSource;
    }

    String expectedSource() {
        return expectedSource;
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add( "logLevel", logLevel.sourceLogLevel() )
                .add("name", name)
                .toString();
    }
}
