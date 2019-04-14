package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;
import java.text.MessageFormat;

/**
 * Commons Logging API: https://commons.apache.org/proper/commons-logging/apidocs/index.html
 */
public class TestClassUsingCommonsLogging_0 {

    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    private int x = 1;

    public void testDebug() {
        someLogger.atFine().log( "test message" );
        someLogger.atFine().log( "%s", new Object() );
        someLogger.atFine().withCause(new Throwable()).log( "test message" );
        someLogger.atFine().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atFine().withCause(new Throwable()).log( "Exception" );
        someLogger.atFine().log( "%s", new Object() );
        someLogger.atFine().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atFine().log( "%s", new Object() );
        someLogger.atFine().withCause(new Throwable()).log( "%s", new Object() );
    }

    public void testError() {
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "test message" );
        someLogger.atSevere().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "Exception" );
        someLogger.atSevere().log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atSevere().log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "%s", new Object() );
    }

    public void testFatal() {
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "test message" );
        someLogger.atSevere().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "Exception" );
        someLogger.atSevere().log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atSevere().log( "%s", new Object() );
        someLogger.atSevere().withCause(new Throwable()).log( "%s", new Object() );
    }

    public void testInfo() {
        someLogger.atInfo().log( "test message" );
        someLogger.atInfo().log( "%s", new Object() );
        someLogger.atInfo().withCause(new Throwable()).log( "test message" );
        someLogger.atInfo().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atInfo().withCause(new Throwable()).log( "Exception" );
        someLogger.atInfo().log( "%s", new Object() );
        someLogger.atInfo().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atInfo().log( "%s", new Object() );
        someLogger.atInfo().withCause(new Throwable()).log( "%s", new Object() );
    }

    public void testTrace() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "%s", new Object() );
        someLogger.atFinest().withCause(new Throwable()).log( "test message" );
        someLogger.atFinest().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atFinest().withCause(new Throwable()).log( "Exception" );
        someLogger.atFinest().log( "%s", new Object() );
        someLogger.atFinest().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atFinest().log( "%s", new Object() );
        someLogger.atFinest().withCause(new Throwable()).log( "%s", new Object() );
    }

    public void testWarn() {
        someLogger.atWarning().log( "test message" );
        someLogger.atWarning().log( "%s", new Object() );
        someLogger.atWarning().withCause(new Throwable()).log( "test message" );
        someLogger.atWarning().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atWarning().withCause(new Throwable()).log( "Exception" );
        someLogger.atWarning().log( "%s", new Object() );
        someLogger.atWarning().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atWarning().log( "%s", new Object() );
        someLogger.atWarning().withCause(new Throwable()).log( "%s", new Object() );
    }

    public void testEnabled() {
        someLogger.atFinest().log( "message" );
        someLogger.atFine().log( "message" );
        someLogger.atInfo().log( "message" );
        someLogger.atWarning().log( "message" );
        someLogger.atSevere().log( "message" );
        someLogger.atSevere().log( "message" );
    }

    public void testMessageFormat() {
        someLogger.atInfo().log( "%s", 10 );
        someLogger.atInfo().log( "a" + 1 + "b" );
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            someLogger.atSevere().withCause(e).log( "The message" );
        }
    }
}
