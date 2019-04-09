package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingSlf4j_0 {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private int x = 1;

    public void testLogLevels() {
        logger.atFinest().log( "test message" );
        logger.atFinest().log( "test message" );

        logger.atFine().log( "test message" );
        logger.atFine().log( "test message" );

        logger.atInfo().log( "test message" );
        logger.atInfo().log( "test message" );

        logger.atWarning().log( "test message" );
        logger.atWarning().log( "test message" );

        logger.atSevere().log( "test message" );
        logger.atSevere().log( "test message" );
    }

    public void testEnabled() {
        logger.atFinest().isEnabled();
        logger.atFinest().isEnabled();

        logger.atFine().isEnabled();
        logger.atFine().isEnabled();

        logger.atInfo().isEnabled();
        logger.atInfo().isEnabled();

        logger.atWarning().isEnabled();
        logger.atWarning().isEnabled();

        logger.atSevere().isEnabled();
        logger.atSevere().isEnabled();
    }

    public void testMessageFormat() {
        logger.atInfo().log("1. Single parameter: %s","abc");
        logger.atInfo().log("2. Escaped formatting anchor: \\{}");
        logger.atInfo().log("3. Escaped anchor and single parameter: {} %s", "abc");
        logger.atInfo().log("4. Escaped anchors and single parameter: {} %s {}", "abc");
        logger.atInfo().log("5. Double-escaped anchor, single parameter: \\%s", "abc");
        logger.atInfo().log("6. Double-escaped anchor, no parameter: \\\\{}");
        logger.atInfo().log("7. Single parameter, double-escaped anchor: %s \\%s", "abc");
        logger.atInfo().log("8. Percent sign: 5%% of %s", "abc");
        logger.atInfo().log( "9. Explicit Object[] %s %s %s", "abc", "def", "ghi" );
        logger.atInfo().withCause(new Throwable()).log( "10. Explicit Object[] with exception %s %s", "abc", "def" );
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch( NullPointerException e ) {
            logger.atSevere().withCause(e).log( "The %s message", "exception" );
        }
    }
    public void testOther() {
        logger.atInfo().log( "a" + 1 + "b" );

        // TODO [LoggerApiRefactoringCheck] Unable to convert message format expression - not a string literal
        logger.atInfo().withCause(new Throwable()).log( "a" + 1 + "b {}", "argument" );

        logger.atInfo().log( "%s", "abc" );
    }
}
