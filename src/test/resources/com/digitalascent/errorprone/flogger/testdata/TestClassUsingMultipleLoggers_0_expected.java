package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingMultipleLoggers_0 {
    private static final FluentLogger logger1 = FluentLogger.forEnclosingClass();


    private final Logger logger2 = LoggerFactory.getLogger("some other logger1");

    public void testMessageFormatArguments() {
        logger1.atInfo().log( "message" );
        logger2.info("message");
    }
}