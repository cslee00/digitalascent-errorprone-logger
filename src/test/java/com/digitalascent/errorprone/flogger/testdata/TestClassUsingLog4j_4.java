package com.digitalascent.errorprone.flogger.testdata;


import org.apache.log4j.Level;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class TestClassUsingLog4j_4 {

    private final Logger someLogger = LogManager.getLogger(TestClassUsingLog4j_4.class.getName());

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.log(Level.TRACE, "test message");
    }

}