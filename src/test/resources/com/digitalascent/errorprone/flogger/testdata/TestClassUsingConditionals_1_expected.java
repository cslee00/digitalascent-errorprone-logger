package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingConditionals_1 {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public void testConditionals() {
        boolean infoEnabled = logger.atInfo().isEnabled();
        if( infoEnabled ) {
            logger.atInfo().log( "info message" );
        }
    }
}