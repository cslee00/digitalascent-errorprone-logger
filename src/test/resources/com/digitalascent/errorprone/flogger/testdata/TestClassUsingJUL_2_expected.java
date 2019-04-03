package com.digitalascent.errorprone.flogger.testdata;

public class TestClassUsingJUL_2 {
    private final Logger someLogger = Logger.getLogger("some logger");

    public void testLogLevels() {
        someLogger.atFinest().log("test message");
        someLogger.atFinest().log("test message");
    }
}