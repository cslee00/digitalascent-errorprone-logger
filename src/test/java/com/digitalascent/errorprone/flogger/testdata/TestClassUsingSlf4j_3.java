package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingSlf4j_3 {

    private final Logger logger = LoggerFactory.getLogger(Object.class);

    public void testLogLevels() {
        logger.trace("test message");
        logger.trace(DummySlf4JMarker.INSTANCE, "test message");
    }
}
