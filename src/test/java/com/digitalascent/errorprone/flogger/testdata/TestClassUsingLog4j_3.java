package com.digitalascent.errorprone.flogger.testdata;


import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TestClassUsingLog4j_3 {

    private final Logger someLogger = LogManager.getLogger(getClass().getName());

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.log(Level.TRACE, "test message");
    }

}
