package com.digitalascent.errorprone.flogger.testdata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.text.MessageFormat;

/**
 * Commons Logging API: https://commons.apache.org/proper/commons-logging/apidocs/index.html
 */
public class TestClassUsingCommonsLogging_0 {

    private int x = 1;

    public void testDebug() {
        someLogger.debug("test message");
        someLogger.debug(new Object());
        someLogger.debug("test message", new Throwable());
        someLogger.debug(new Object(), new Throwable());
        someLogger.debug(new Throwable());
        someLogger.debug(String.format("%s",new Object()));
        someLogger.debug(String.format("%s",new Object()), new Throwable());
        someLogger.debug(MessageFormat.format("{0}", new Object()));
        someLogger.debug(MessageFormat.format("{0}", new Object()), new Throwable());
    }

    public void testError() {
        someLogger.error("test message");
        someLogger.error(new Object());
        someLogger.error("test message", new Throwable());
        someLogger.error(new Object(), new Throwable());
        someLogger.error(new Throwable());
        someLogger.error(String.format("%s",new Object()));
        someLogger.error(String.format("%s",new Object()), new Throwable());
        someLogger.error(MessageFormat.format("{0}", new Object()));
        someLogger.error(MessageFormat.format("{0}", new Object()), new Throwable());
    }

    public void testFatal() {
        someLogger.fatal("test message");
        someLogger.fatal(new Object());
        someLogger.fatal("test message", new Throwable());
        someLogger.fatal(new Object(), new Throwable());
        someLogger.fatal(new Throwable());
        someLogger.fatal(String.format("%s",new Object()));
        someLogger.fatal(String.format("%s",new Object()), new Throwable());
        someLogger.fatal(MessageFormat.format("{0}", new Object()));
        someLogger.fatal(MessageFormat.format("{0}", new Object()), new Throwable());
    }

    public void testInfo() {
        someLogger.info("test message");
        someLogger.info(new Object());
        someLogger.info("test message", new Throwable());
        someLogger.info(new Object(), new Throwable());
        someLogger.info(new Throwable());
        someLogger.info(String.format("%s",new Object()));
        someLogger.info(String.format("%s",new Object()), new Throwable());
        someLogger.info(MessageFormat.format("{0}", new Object()));
        someLogger.info(MessageFormat.format("{0}", new Object()), new Throwable());
    }

    public void testTrace() {
        someLogger.trace("test message");
        someLogger.trace(new Object());
        someLogger.trace("test message", new Throwable());
        someLogger.trace(new Object(), new Throwable());
        someLogger.trace(new Throwable());
        someLogger.trace(String.format("%s",new Object()));
        someLogger.trace(String.format("%s",new Object()), new Throwable());
        someLogger.trace(MessageFormat.format("{0}", new Object()));
        someLogger.trace(MessageFormat.format("{0}", new Object()), new Throwable());
    }

    public void testWarn() {
        someLogger.warn("test message");
        someLogger.warn(new Object());
        someLogger.warn("test message", new Throwable());
        someLogger.warn(new Object(), new Throwable());
        someLogger.warn(new Throwable());
        someLogger.warn(String.format("%s",new Object()));
        someLogger.warn(String.format("%s",new Object()), new Throwable());
        someLogger.warn(MessageFormat.format("{0}", new Object()));
        someLogger.warn(MessageFormat.format("{0}", new Object()), new Throwable());
    }

    public void testEnabled() {
        if (someLogger.isTraceEnabled()) {
            someLogger.trace("message");
        }
        if (someLogger.isDebugEnabled()) {
            someLogger.debug("message");
        }
        if (someLogger.isInfoEnabled()) {
            someLogger.info("message");
        }
        if (someLogger.isWarnEnabled()) {
            someLogger.warn("message");
        }
        if (someLogger.isErrorEnabled()) {
            someLogger.error("message");
        }
        if (someLogger.isFatalEnabled()) {
            someLogger.fatal("message");
        }
    }

    public void testMessageFormat() {
        someLogger.info(10);
        someLogger.info("a" + 1 + "b");
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            someLogger.error("The message", e);
        }
    }

    private final Log someLogger = LogFactory.getLog(getClass());
}
