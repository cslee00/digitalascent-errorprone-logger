package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingCommonsLogging_3 {
    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    public void testLogLevels() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFine().log( "test message" );
        someLogger.atInfo().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
    }

}