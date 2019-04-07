package com.digitalascent.errorprone.flogger.testdata;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestClassUsingLog4j2_7 {

    private int x = 1;

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.trace(DummyLog4J2Marker.INSTANCE, "test message");
        someLogger.log(Level.TRACE, "test message");
        someLogger.log(Level.TRACE, DummyLog4J2Marker.INSTANCE, "test message");

        someLogger.debug("test message");
        someLogger.debug(DummyLog4J2Marker.INSTANCE, "test message");
        someLogger.log(Level.DEBUG, "test message");
        someLogger.log(Level.DEBUG, DummyLog4J2Marker.INSTANCE, "test message");

        someLogger.info("test message");
        someLogger.info(DummyLog4J2Marker.INSTANCE, "test message");
        someLogger.log(Level.INFO, "test message");
        someLogger.log(Level.INFO, DummyLog4J2Marker.INSTANCE, "test message");

        someLogger.warn("test message");
        someLogger.warn(DummyLog4J2Marker.INSTANCE, "test message");
        someLogger.log(Level.WARN, "test message");
        someLogger.log(Level.WARN, DummyLog4J2Marker.INSTANCE, "test message");

        someLogger.error("test message");
        someLogger.error(DummyLog4J2Marker.INSTANCE, "test message");
        someLogger.log(Level.ERROR, "test message");
        someLogger.log(Level.ERROR, DummyLog4J2Marker.INSTANCE, "test message");

        someLogger.fatal("test message");
        someLogger.fatal(DummyLog4J2Marker.INSTANCE, "test message");
        someLogger.log(Level.FATAL, "test message");
        someLogger.log(Level.FATAL, DummyLog4J2Marker.INSTANCE, "test message");
    }

    public void testEnabled() {
        someLogger.isTraceEnabled();
        someLogger.isTraceEnabled(DummyLog4J2Marker.INSTANCE);
        someLogger.isEnabled(Level.TRACE);
        someLogger.isEnabled(Level.TRACE, DummyLog4J2Marker.INSTANCE);

        someLogger.isDebugEnabled();
        someLogger.isDebugEnabled(DummyLog4J2Marker.INSTANCE);
        someLogger.isEnabled(Level.DEBUG);
        someLogger.isEnabled(Level.DEBUG, DummyLog4J2Marker.INSTANCE);

        someLogger.isInfoEnabled();
        someLogger.isInfoEnabled(DummyLog4J2Marker.INSTANCE);
        someLogger.isEnabled(Level.INFO);
        someLogger.isEnabled(Level.INFO, DummyLog4J2Marker.INSTANCE);

        someLogger.isWarnEnabled();
        someLogger.isWarnEnabled(DummyLog4J2Marker.INSTANCE);
        someLogger.isEnabled(Level.WARN);
        someLogger.isEnabled(Level.WARN, DummyLog4J2Marker.INSTANCE);

        someLogger.isErrorEnabled();
        someLogger.isErrorEnabled(DummyLog4J2Marker.INSTANCE);
        someLogger.isEnabled(Level.ERROR);
        someLogger.isEnabled(Level.ERROR, DummyLog4J2Marker.INSTANCE);

        someLogger.isFatalEnabled();
        someLogger.isFatalEnabled(DummyLog4J2Marker.INSTANCE);
        someLogger.isEnabled(Level.FATAL);
        someLogger.isEnabled(Level.FATAL, DummyLog4J2Marker.INSTANCE);
    }

    public void testMessageFormat() {
        someLogger.info("1. Single parameter: %s", "abc");
        someLogger.info("2. Escaped formatting anchor: %%s");
        someLogger.info("3. Escaped anchor and single parameter: %%s %s", "abc");
        someLogger.info("4. Escaped anchors and single parameter: %%s %s %%s", "abc");
        someLogger.info("6. Double-escaped anchor, no parameter: %%%s");
        someLogger.info("8. Percent sign: 5%% of %s", "abc");
        someLogger.info("9. Object[] %s %s", new Object[] { "abc", "def" });
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            someLogger.error("The %s message", "exception", e);
        }
    }

    public void testOther() {
        someLogger.info("a" + 1 + "b");
        someLogger.info("a" + 1 + "b %s", "argument", new Throwable());
        someLogger.info(new Object());
        someLogger.info(new Object(), new Throwable());
        someLogger.info(String.format("%s","abc"));
    }

    private final Logger someLogger = LogManager.getFormatterLogger(getClass());
}