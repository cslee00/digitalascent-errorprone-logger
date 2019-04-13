package com.digitalascent.errorprone.flogger.testdata;


import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TestClassUsingLog4j_0 {

    private int x = 1;

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.log(Level.TRACE, "test message");

        someLogger.debug("test message");
        someLogger.log(Level.DEBUG, "test message");

        someLogger.info("test message");
        someLogger.log(Level.INFO, "test message");

        someLogger.warn("test message");
        someLogger.log(Level.WARN, "test message");

        someLogger.error("test message");
        someLogger.log(Level.ERROR, "test message");

        someLogger.fatal("test message");
        someLogger.log(Level.FATAL, "test message");
    }

    public void testEnabled() {
        if (someLogger.isTraceEnabled()) {
            someLogger.trace("message");
        }
        if (someLogger.isEnabledFor(Level.TRACE)) {
            someLogger.trace("message");
        }

        if (someLogger.isDebugEnabled()) {
            someLogger.debug("message");
        }
        if (someLogger.isEnabledFor(Level.DEBUG)) {
            someLogger.debug("message");
        }

        if (someLogger.isInfoEnabled()) {
            someLogger.info("message");
        }
        if (someLogger.isEnabledFor(Level.INFO)) {
            someLogger.info("message");
        }

        if (someLogger.isEnabledFor(Level.WARN)) {
            someLogger.warn("message");
        }

        if (someLogger.isEnabledFor(Level.ERROR)) {
            someLogger.error("message");
        }

        if (someLogger.isEnabledFor(Level.FATAL)) {
            someLogger.fatal("message");
        }
    }

    public void testMessageFormat() {
        someLogger.info(new Object());
        someLogger.info("some string");
        someLogger.info(10);
        someLogger.info("a" + 1 + "b");
        someLogger.info(String.format("%s", "abc"));
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            someLogger.error("The message", e);
        }
    }

    private final Logger someLogger = LogManager.getLogger(getClass());
}
