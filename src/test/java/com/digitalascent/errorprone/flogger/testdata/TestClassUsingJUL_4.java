package com.digitalascent.errorprone.flogger.testdata;


import java.util.logging.Level;
import java.util.logging.Logger;

public class TestClassUsingJUL_4 {

    private static final Logger someLogger = Logger.getLogger(TestClassUsingJUL_4.class.getName());

    public void testLogLevels() {
        someLogger.finest("test message");
        someLogger.log(Level.FINEST, "test message");
    }
}
