package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingMultipleLoggers_0 {
    private final Logger logger1 = LoggerFactory.getLogger(getClass());
    private final Logger logger2 = LoggerFactory.getLogger("some other logger1");

     public void testMessageFormatArguments() {
         logger1.info("message" );
         logger2.info("message");
    }
}
