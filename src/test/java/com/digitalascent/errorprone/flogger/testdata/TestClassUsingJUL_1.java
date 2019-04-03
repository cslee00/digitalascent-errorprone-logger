package com.digitalascent.errorprone.flogger.testdata;


import java.util.logging.Level;
import java.util.logging.Logger;

public class TestClassUsingJUL_1 {

    private final Logger someLogger = Logger.getLogger("com.digitalascent.errorprone.flogger.testdata.TestClassUsingJUL_1");

    public void testLogLevels() {
        someLogger.finest("test message");
        someLogger.log(Level.FINEST, "test message");
    }
}
