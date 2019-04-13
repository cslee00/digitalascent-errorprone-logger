package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingConditionals_1 {

    private final Logger logger = LoggerFactory.getLogger(getClass());

     public void testConditionals() {
         boolean infoEnabled = logger.isInfoEnabled();
         if( infoEnabled ) {
             logger.info("info message");
         }
    }
}
