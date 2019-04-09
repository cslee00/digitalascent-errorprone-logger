package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingMultipleLoggers_2 {
    private static final FluentLogger logger0 = FluentLogger.forEnclosingClass();

    private final Logger slf4jLogger = LoggerFactory.getLogger(getClass());
    private final Logger slf4jLogger2 = LoggerFactory.getLogger(getClass());

     public void testMessageFormatArguments() {
         logger0.atInfo().log("message");
         slf4jLogger.info("slf4j message" );
         slf4jLogger2.info("slf4j message" );
    }
}
