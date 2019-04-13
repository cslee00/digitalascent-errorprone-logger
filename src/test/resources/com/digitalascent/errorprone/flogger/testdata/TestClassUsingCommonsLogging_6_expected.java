package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingCommonsLogging_6 {
    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();



    public void testConditionalLogging() {
        someLogger.atFinest().log( "test message" );

        someLogger.atFine().log( "test message" );

        someLogger.atInfo().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
    }

    public void testConditionalLoggingMultiple() {
        someLogger.atFinest().log( "test message" );
        someLogger.atFinest().log( "test message" );

        someLogger.atFine().log( "test message" );
        someLogger.atFine().log( "test message" );

        someLogger.atInfo().log( "test message" );
        someLogger.atInfo().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atWarning().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
        someLogger.atSevere().log( "test message" );
    }

    public void testConditionalLoggingMultipleEmpty() {
    }

    public void testConditionalLoggingMultiple2() {
        if( someLogger.atFinest().isEnabled() ) {
            someLogger.atFinest().log( "test message" );
            someOtherMethod();
        }

        if( someLogger.atFine().isEnabled() ) {
            someLogger.atFine().log( "test message" );
            someOtherMethod();
        }

        if( someLogger.atInfo().isEnabled() ) {
            someLogger.atInfo().log( "test message" );
            someOtherMethod();
        }
        if( someLogger.atWarning().isEnabled()) {
            someLogger.atWarning().log( "test message" );
            someOtherMethod();
        }
        if( someLogger.atSevere().isEnabled()) {
            someLogger.atSevere().log( "test message" );
            someOtherMethod();
        }
        if( someLogger.atSevere().isEnabled()) {
            someLogger.atSevere().log( "test message" );
            someOtherMethod();
        }
    }

    private void someOtherMethod() {

    }
}