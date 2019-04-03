package com.digitalascent.errorprone.flogger.testdata;




public class TestClassUsingLog4j2_5 {

    // TODO [LoggerApiRefactoring] Unable to migrate logger variable
    private final Logger someLogger = LogManager.getLogger("random logger name");

    public void testLogLevels() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );
    }
}