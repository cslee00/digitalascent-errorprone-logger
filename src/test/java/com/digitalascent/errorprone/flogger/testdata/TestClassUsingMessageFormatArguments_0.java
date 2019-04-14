package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.MessageFormat;
import java.util.Arrays;

public class TestClassUsingMessageFormatArguments_0 {
    private final Logger logger = LoggerFactory.getLogger(getClass());

     public void testMessageFormatArguments() {
         logger.info("{}", new Object().toString());
         logger.info("{}", new Object().toString().toString().toString());
         logger.info("{}", Arrays.toString( new Object[] { "abc", "def", "ghi"}));
         logger.info(MessageFormat.format("{0}", new Object[] { "abc" }));
         logger.info(String.format("%s", new Object[] { "abc" }));
    }
}
