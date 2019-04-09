package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingMultipleLoggers_2 {
    private static final FluentLogger logger0 = FluentLogger.forEnclosingClass();

    public void testMessageFormatArguments() {
        logger0.atInfo().log("message");
        logger0.atInfo().log( "slf4j message" );
        logger0.atInfo().log( "slf4j message" );
    }
}