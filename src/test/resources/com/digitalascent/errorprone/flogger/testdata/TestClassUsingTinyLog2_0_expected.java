package com.digitalascent.errorprone.flogger.testdata;


import com.google.common.flogger.FluentLogger;

public class TestClassUsingTinyLog2_0 {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    private int x = 1;

    public void testLogLevels() {
        logger.atFinest().log( "test message" );

        logger.atFine().log( "test message" );

        logger.atInfo().log( "test message" );

        logger.atWarning().log( "test message" );

        logger.atSevere().log( "test message" );
    }


    public void testMessageFormat() {
        logger.atInfo().log( "Test %s argument", "some" );

        logger.atInfo().log( "Test %s argument %s", "some", "other" );

        logger.atInfo().log( "Test %s argument %s 5%%", "some", "other" );

        logger.atInfo().log( "Test \\%s argument %s", "other" );

        logger.atInfo().log( "Test \\%s argument %s", "some", "other" );
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

// TODO [LoggerApiRefactoring] Unable to convert message format expression - not a string literal
        logger.atInfo().withCause(new Throwable()).log( "a" + 1 + "b {}", "argument" );

        logger.atFine().log( "message" );
    }
}