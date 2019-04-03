package com.digitalascent.errorprone.flogger.testdata;

public class TestClassUsingLog4j_6 {

    // TODO [LoggerApiRefactoring] Unable to migrate logger variable
    private final Logger someLogger = LogManager.getLogger("some random logger");

    public void testLogLevels() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );
    }
}