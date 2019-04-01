package com.digitalascent.errorprone.flogger.testdata;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestClassUsingSlf4j_0 {

    private int x = 1;

    public void testLogLevels() {
        logger.trace("test message");
        logger.trace(DummySlf4JMarker.INSTANCE, "test message");

        logger.debug("test message");
        logger.debug(DummySlf4JMarker.INSTANCE, "test message");

        logger.info("test message");
        logger.info(DummySlf4JMarker.INSTANCE, "test message");

        logger.warn("test message");
        logger.warn(DummySlf4JMarker.INSTANCE, "test message");

        logger.error("test message");
        logger.error(DummySlf4JMarker.INSTANCE, "test message");
    }

    public void testEnabled() {
        logger.isTraceEnabled();
        logger.isTraceEnabled(DummySlf4JMarker.INSTANCE);

        logger.isDebugEnabled();
        logger.isDebugEnabled(DummySlf4JMarker.INSTANCE);

        logger.isInfoEnabled();
        logger.isInfoEnabled(DummySlf4JMarker.INSTANCE);

        logger.isWarnEnabled();
        logger.isWarnEnabled(DummySlf4JMarker.INSTANCE);

        logger.isErrorEnabled();
        logger.isErrorEnabled(DummySlf4JMarker.INSTANCE);
    }

    public void testMessageFormat() {
        // see Slf4jOutput for runtime examples
        logger.info("1. Single parameter: {}","abc");
        logger.info("2. Escaped formatting anchor: \\{}");
        logger.info("3. Escaped anchor and single parameter: \\{} {}", "abc");
        logger.info("4. Escaped anchors and single parameter: \\{} {} \\{}", "abc");
        logger.info("5. Double-escaped anchor, single parameter: \\\\{}", "abc");
        logger.info("6. Double-escaped anchor, no parameter: \\\\{}");
        logger.info("7. Single parameter, double-escaped anchor: {} \\\\{}", "abc");
        logger.info("8. Percent sign: 5% of {}", "abc");
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            logger.error("The {} message", "exception", e);
        }
    }

    public void testOther() {
        logger.info("a" + 1 + "b");
        logger.info("a" + 1 + "b {}", "argument", new Throwable());
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
}
