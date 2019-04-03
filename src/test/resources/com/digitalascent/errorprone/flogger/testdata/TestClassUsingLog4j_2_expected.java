package com.digitalascent.errorprone.flogger.testdata;

public class TestClassUsingLog4j_2 {

    // TODO [LoggerApiRefactoring] Unable to migrate logger variable
    private final Logger someLogger = LogManager.getLogger(Object.class);

    public void testLogLevels() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );
    }
}