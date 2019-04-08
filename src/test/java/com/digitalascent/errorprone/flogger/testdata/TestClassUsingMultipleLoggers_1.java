package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingMultipleLoggers_1 {
    private static final FluentLogger logger0 = FluentLogger.forEnclosingClass();

    private final Logger logger1 = LoggerFactory.getLogger(getClass());
    private final Logger logger2 = LoggerFactory.getLogger("some other logger1");

     public void testMessageFormatArguments() {
         logger0.atInfo().log("message");
         logger1.info("message" );
         logger2.info("message");
    }
}
