package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;

public class TestClassUsingStringConcatenation_0 {
    private final Logger logger = LoggerFactory.getLogger(getClass());

     public void testStringConcatentation() {
         String x = "foo";
         logger.info("a" + 1 + "b: " + x);
         logger.info("a" + 1 + "b", "abc");
    }
}
