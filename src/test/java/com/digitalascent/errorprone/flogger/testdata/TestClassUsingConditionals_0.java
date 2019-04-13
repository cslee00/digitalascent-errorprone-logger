package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingConditionals_0 {

    private final Logger logger = LoggerFactory.getLogger(getClass());

     public void testConditionals() {
         if( logger.isInfoEnabled() ) {
             logger.info("info message");
         }

         if( logger.isInfoEnabled()) {
             // deliberate mismatch of log level
             logger.debug("debug message");
         }
    }
}
