package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingMultipleLoggers_1 {
    private static final FluentLogger logger0 = FluentLogger.forEnclosingClass();

    private final Logger logger2 = LoggerFactory.getLogger("some other logger1");

    public void testMessageFormatArguments() {
        logger0.atInfo().log("message");
        logger0.atInfo().log( "message" );
        logger2.info("message");
    }
}