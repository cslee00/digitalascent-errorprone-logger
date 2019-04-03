package com.digitalascent.errorprone.flogger.testdata;


import java.util.logging.Level;
import java.util.logging.Logger;

public class TestClassUsingJUL_3 {

    private final Logger someLogger = Logger.getLogger(TestClassUsingJUL_3.class.getName());

    public void testLogLevels() {
        someLogger.finest("test message");
        someLogger.log(Level.FINEST, "test message");
    }
}
