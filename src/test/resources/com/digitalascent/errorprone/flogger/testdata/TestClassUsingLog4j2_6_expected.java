package com.digitalascent.errorprone.flogger.testdata;


import com.google.common.flogger.FluentLogger;

public class TestClassUsingLog4j2_6 {

    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    public void testLogLevels() {
        someLogger.atFinest().log("test message");
        someLogger.atFinest().log("test message");
        someLogger.atFinest().log("test message");
        someLogger.atFinest().log("test message");
    }
}