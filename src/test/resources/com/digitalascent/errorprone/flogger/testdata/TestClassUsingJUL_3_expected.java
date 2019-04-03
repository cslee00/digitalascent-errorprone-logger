package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingJUL_3 {

    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    public void testLogLevels() {
        someLogger.atFinest().log("test message");
        someLogger.atFinest().log("test message");
    }
}