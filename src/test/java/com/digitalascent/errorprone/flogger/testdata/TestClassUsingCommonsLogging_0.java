package com.digitalascent.errorprone.flogger.testdata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestClassUsingCommonsLogging_0 {

    private int x = 1;

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.debug("test message");
        someLogger.info("test message");
        someLogger.warn("test message");
        someLogger.error("test message");
        someLogger.fatal("test message");
    }

    public void testEnabled() {
        someLogger.isTraceEnabled();
        someLogger.isDebugEnabled();
        someLogger.isInfoEnabled();
        someLogger.isWarnEnabled();
        someLogger.isErrorEnabled();
        someLogger.isFatalEnabled();
    }

    public void testMessageFormat() {
        someLogger.info(new Object());
        someLogger.info("some string");
        someLogger.info(10);
        someLogger.info("a" + 1 + "b");
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch( NullPointerException e ) {
            someLogger.error("The message",  e);
        }
    }

    public void testOther() {
        someLogger.info(new Object());
        someLogger.info(new Object(), new Throwable());
        someLogger.info(new Throwable());
        someLogger.info(String.format("%s","abc"));
    }

    private final Log someLogger = LogFactory.getLog(getClass());
}
