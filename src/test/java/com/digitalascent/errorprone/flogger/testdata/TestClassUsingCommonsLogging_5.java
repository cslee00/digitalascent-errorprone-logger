package com.digitalascent.errorprone.flogger.testdata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestClassUsingCommonsLogging_5 {
    private static final Log someLogger = LogFactory.getLog(TestClassUsingCommonsLogging_5.class);

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.debug("test message");
        someLogger.info("test message");
        someLogger.warn("test message");
        someLogger.error("test message");
        someLogger.fatal("test message");
    }

}
