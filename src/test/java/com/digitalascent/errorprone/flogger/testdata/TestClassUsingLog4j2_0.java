package com.digitalascent.errorprone.flogger.testdata;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.message.Message;

public class TestClassUsingLog4j2_0 {

    private int x = 1;

    // TODO - catching, throwing
    // TODO - entry, traceEntry, exit, traceExit

    public void testLogLevels() {
        // TODO - Message, MessageSupplier, Supplier msgSupplier, Object message
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
        if (someLogger.isTraceEnabled()) {
            someLogger.trace("message");
        }
        if (someLogger.isTraceEnabled(DummyLog4J2Marker.INSTANCE)) {
            someLogger.trace("message");
        }
        if (someLogger.isEnabled(Level.TRACE)) {
            someLogger.trace("message");
        }
        if (someLogger.isEnabled(Level.TRACE, DummyLog4J2Marker.INSTANCE)) {
            someLogger.trace("message");
        }

        if (someLogger.isDebugEnabled()) {
            someLogger.debug("message");
        }
        if (someLogger.isDebugEnabled(DummyLog4J2Marker.INSTANCE)) {
            someLogger.debug("message");
        }
        if (someLogger.isEnabled(Level.DEBUG)) {
            someLogger.debug("message");
        }
        if (someLogger.isEnabled(Level.DEBUG, DummyLog4J2Marker.INSTANCE)) {
            someLogger.debug("message");
        }

        if (someLogger.isInfoEnabled()) {
            someLogger.info("message");
        }
        if (someLogger.isInfoEnabled(DummyLog4J2Marker.INSTANCE)) {
            someLogger.info("message");
        }
        if (someLogger.isEnabled(Level.INFO)) {
            someLogger.info("message");
        }
        if (someLogger.isEnabled(Level.INFO, DummyLog4J2Marker.INSTANCE)) {
            someLogger.info("message");
        }

        if (someLogger.isWarnEnabled()) {
            someLogger.warn("message");
        }
        if (someLogger.isWarnEnabled(DummyLog4J2Marker.INSTANCE)) {
            someLogger.warn("message");
        }
        if (someLogger.isEnabled(Level.WARN)) {
            someLogger.warn("message");
        }
        if (someLogger.isEnabled(Level.WARN, DummyLog4J2Marker.INSTANCE)) {
            someLogger.warn("message");
        }

        if (someLogger.isErrorEnabled()) {
            someLogger.error("message");
        }
        if (someLogger.isErrorEnabled(DummyLog4J2Marker.INSTANCE)) {
            someLogger.error("message");
        }
        if (someLogger.isEnabled(Level.ERROR)) {
            someLogger.error("message");
        }
        if (someLogger.isEnabled(Level.ERROR, DummyLog4J2Marker.INSTANCE)) {
            someLogger.error("message");
        }

        if (someLogger.isFatalEnabled()) {
            someLogger.fatal("message");
        }
        if (someLogger.isFatalEnabled(DummyLog4J2Marker.INSTANCE)) {
            someLogger.fatal("message");
        }
        if (someLogger.isEnabled(Level.FATAL)) {
            someLogger.fatal("message");
        }
        if (someLogger.isEnabled(Level.FATAL, DummyLog4J2Marker.INSTANCE)) {
            someLogger.fatal("message");
        }
    }

    public void testMessageFormat() {
        someLogger.info("1. Single parameter: {}", "abc");
        someLogger.info("2. Escaped formatting anchor: \\{}");
        someLogger.info("3. Escaped anchor and single parameter: \\{} {}", "abc");
        someLogger.info("4. Escaped anchors and single parameter: \\{} {} \\{}", "abc");
        someLogger.info("5. Double-escaped anchor, single parameter: \\\\{}", "abc");
        someLogger.info("6. Double-escaped anchor, no parameter: \\\\{}");
        someLogger.info("7. Single parameter, double-escaped anchor: {} \\\\{}", "abc");
        someLogger.info("8. Percent sign: 5% of {}", "abc");
        someLogger.info("9. Object[] {} {}", new Object[]{"abc", "def"});
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            someLogger.error("The {} message", "exception", e);
        }
    }

    public void testOther() {
        someLogger.info("a" + 1 + "b");
        someLogger.info("a" + 1 + "b {}", "extract", new Throwable());
        someLogger.info(new Object());
        someLogger.info(new Object(), new Throwable());
        someLogger.info(String.format("%s", "abc"));
        someLogger.info(() -> "foo");
        someLogger.info(myMessage);
        someLogger.printf(Level.TRACE, "abc {}", "param1");
    }

    private final Message myMessage = new Message() {

        @Override
        public String getFormattedMessage() {
            return "formatted message";
        }

        @Override
        public String getFormat() {
            return null;
        }

        @Override
        public Object[] getParameters() {
            return new Object[0];
        }

        @Override
        public Throwable getThrowable() {
            return null;
        }
    };

    private final Logger someLogger = LogManager.getLogger(getClass());
}
