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
        if (logger.isTraceEnabled()) {
            logger.trace("message");
        }
        if (logger.isTraceEnabled(DummySlf4JMarker.INSTANCE)) {
            logger.trace("message");
        }

        if (logger.isDebugEnabled()) {
            logger.debug("message");
        }
        if (logger.isDebugEnabled(DummySlf4JMarker.INSTANCE)) {
            logger.debug("message");
        }

        if (logger.isInfoEnabled()) {
            logger.info("message");
        }
        if (logger.isInfoEnabled(DummySlf4JMarker.INSTANCE)) {
            logger.info("message");
        }

        if (logger.isWarnEnabled()) {
            logger.warn("message");
        }
        if (logger.isWarnEnabled(DummySlf4JMarker.INSTANCE)) {
            logger.warn("message");
        }

        if (logger.isErrorEnabled()) {
            logger.error("message");
        }
        if (logger.isErrorEnabled(DummySlf4JMarker.INSTANCE)) {
            logger.error("message");
        }
    }

    public void testMessageFormat() {
        // see Slf4jOutput for runtime examples
        logger.info("1. Single parameter: {}", "abc");
        logger.info("2. Escaped formatting anchor: \\{}");
        logger.info("3. Escaped anchor and single parameter: \\{} {}", "abc");
        logger.info("4. Escaped anchors and single parameter: \\{} {} \\{}", "abc");
        logger.info("5. Double-escaped anchor, single parameter: \\\\{}", "abc");
        logger.info("6. Double-escaped anchor, no parameter: \\\\{}");
        logger.info("7. Single parameter, double-escaped anchor: {} \\\\{}", "abc");
        logger.info("8. Percent sign: 5% of {}", "abc");
        logger.info("9. Explicit Object[] {} {} {}", new Object[]{"abc", "def", "ghi"});
        logger.info("10. Explicit Object[] with exception {} {}", new Object[]{"abc", "def", new Throwable()});
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
        logger.info(String.format("%s", "abc"));
    }

    private final Logger logger = LoggerFactory.getLogger(getClass());
}
