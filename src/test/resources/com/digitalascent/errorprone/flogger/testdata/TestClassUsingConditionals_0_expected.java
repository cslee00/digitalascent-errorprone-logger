package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingConditionals_0 {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public void testConditionals() {
        logger.atInfo().log( "info message" );

        logger.atFine().log( "debug message" );
    }
}