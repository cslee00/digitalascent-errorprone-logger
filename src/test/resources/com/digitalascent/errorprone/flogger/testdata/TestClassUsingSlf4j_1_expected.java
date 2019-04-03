package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingSlf4j_1 {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public void testLogLevels() {
        logger.atFinest().log( "test message" );
        logger.atFinest().log( "test message" );
    }
}
