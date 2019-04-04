package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.*;

public class TestClassUsingSlf4j_7 {

    private static final Logger logger = LoggerFactory.getLogger(TestClassUsingSlf4j_7.class);

    public void testLogLevels() {
        logger.trace("test message");
        logger.trace(DummySlf4JMarker.INSTANCE, "test message");
    }
}
