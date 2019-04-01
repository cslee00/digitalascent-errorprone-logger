package com.digitalascent.errorprone.flogger.testdata;


import com.google.common.flogger.FluentLogger;

public class TestClassUsingJUL_0 {

    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    private int x = 1;

    // TODO - catching, throwing
    // TODO - entry, traceEntry, exit, traceExit

    public void testLogLevels() {
        // TODO - Message, MessageSupplier, Supplier msgSupplier, Object message
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );

        someLogger.atFine().log( "test message" );
        someLogger.atFine().log( "test message" );
        someLogger.atFine().log( "test message" );
        someLogger.atFine().log( "test message" );

        someLogger.atInfo().log( "test message" );
        someLogger.atInfo().log( "test message" );
        someLogger.atInfo().log( "test message" );
        someLogger.atInfo().log( "test message" );

        someLogger.atWarning().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atWarning().log( "test message" );

        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );

        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
    }

    public void testEnabled() {
        someLogger.atFinest().isEnabled();
        someLogger.atFinest().isEnabled();
        someLogger.atFinest().isEnabled();
        someLogger.atFinest().isEnabled();

        someLogger.atFine().isEnabled();
        someLogger.atFine().isEnabled();
        someLogger.atFine().isEnabled();
        someLogger.atFine().isEnabled();

        someLogger.atInfo().isEnabled();
        someLogger.atInfo().isEnabled();
        someLogger.atInfo().isEnabled();
        someLogger.atInfo().isEnabled();

        someLogger.atWarning().isEnabled();
        someLogger.atWarning().isEnabled();
        someLogger.atWarning().isEnabled();
        someLogger.atWarning().isEnabled();

        someLogger.atSevere().isEnabled();
        someLogger.atSevere().isEnabled();
        someLogger.atSevere().isEnabled();
        someLogger.atSevere().isEnabled();

        someLogger.atSevere().isEnabled();
        someLogger.atSevere().isEnabled();
        someLogger.atSevere().isEnabled();
        someLogger.atSevere().isEnabled();
    }

    public void testMessageFormat() {
        someLogger.atInfo().log("1. Single parameter: %s","abc");
        someLogger.atInfo().log("2. Escaped formatting anchor: \\{}");
        someLogger.atInfo().log("3. Escaped anchor and single parameter: {} %s", "abc");
        someLogger.atInfo().log("4. Escaped anchors and single parameter: {} %s {}", "abc");
        someLogger.atInfo().log("5. Double-escaped anchor, single parameter: \\%s", "abc");
        someLogger.atInfo().log("6. Double-escaped anchor, no parameter: \\\\{}");
        someLogger.atInfo().log("7. Single parameter, double-escaped anchor: %s \\%s", "abc");
        someLogger.atInfo().log("8. Percent sign: 5%% of %s", "abc");
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch( NullPointerException e ) {
            someLogger.atSevere().withCause(e).log( "The %s message", "exception" );
        }
    }
    public void testOther() {
        someLogger.atInfo().log( "a" + 1 + "b" );

// TODO [LoggerApiRefactoring] Unable to convert message format expression - not a string literal
        someLogger.atInfo().withCause(new Throwable()).log( "a" + 1 + "b {}", "argument" );
    }
}