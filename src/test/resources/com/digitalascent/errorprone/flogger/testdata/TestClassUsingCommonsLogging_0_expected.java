package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingCommonsLogging_0 {

    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    private int x = 1;

    public void testLogLevels() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFine().log( "test message" );
        someLogger.atInfo().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
    }

    public void testEnabled() {
        someLogger.atFinest().isEnabled();
        someLogger.atFine().isEnabled();
        someLogger.atInfo().isEnabled();
        someLogger.atWarning().isEnabled();
        someLogger.atSevere().isEnabled();
        someLogger.atSevere().isEnabled();
    }

    public void testMessageFormat() {
        someLogger.atInfo().log( "%s", new Object() );
        someLogger.atInfo().log( "some string" );
        someLogger.atInfo().log( "%s", 10 );
        someLogger.atInfo().log( "a" + 1 + "b" );
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch( NullPointerException e ) {
            someLogger.atSevere().withCause(e).log( "The message" );
        }
    }
}
