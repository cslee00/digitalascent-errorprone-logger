package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingSlf4j_5 {

    private final Logger logger = LoggerFactory.getLogger("some random logger");

    public void testLogLevels() {
        logger.trace("test message");
        logger.trace(DummySlf4JMarker.INSTANCE, "test message");
    }
}
