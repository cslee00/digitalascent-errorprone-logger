package com.digitalascent.errorprone.flogger.testdata;


import com.google.common.flogger.FluentLogger;

public class TestClassUsingLog4j2_7 {

    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    private int x = 1;

    public void testLogLevels() {
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
        someLogger.atInfo().log("2. Escaped formatting anchor: %%s");
        someLogger.atInfo().log("3. Escaped anchor and single parameter: %%s %s", "abc");
        someLogger.atInfo().log("4. Escaped anchors and single parameter: %%s %s %%s", "abc");
        someLogger.atInfo().log("6. Double-escaped anchor, no parameter: %%%s");
        someLogger.atInfo().log("8. Percent sign: 5%% of %s", "abc");
        someLogger.atInfo().log( "9. Object[] %s %s", "abc", "def" );
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

// TODO [LoggerApiRefactoringCheck] Unable to convert message format expression - not a string literal
        someLogger.atInfo().withCause(new Throwable()).log( "a" + 1 + "b %s", "argument" );
        someLogger.atInfo().log( "%s", new Object() );
        someLogger.atInfo().withCause(new Throwable()).log( "%s", new Object() );
        someLogger.atInfo().log( "%s", "abc" );
    }
}