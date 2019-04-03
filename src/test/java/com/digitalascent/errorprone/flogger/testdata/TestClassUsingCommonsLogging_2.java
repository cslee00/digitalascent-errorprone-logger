package com.digitalascent.errorprone.flogger.testdata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestClassUsingCommonsLogging_2 {
    private final Log someLogger = LogFactory.getLog("com.digitalascent.errorprone.flogger.testdata.TestClassUsingCommonsLogging_2");

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.debug("test message");
        someLogger.info("test message");
        someLogger.warn("test message");
        someLogger.error("test message");
        someLogger.fatal("test message");
    }

}
