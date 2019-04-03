package com.digitalascent.errorprone.flogger.testdata;



public class TestClassUsingCommonsLogging_4 {
    private final Log someLogger = LogFactory.getLog("some other logger name");

    public void testLogLevels() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFine().log( "test message" );
        someLogger.atInfo().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
    }

}