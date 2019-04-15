package com.digitalascent.errorprone.flogger.testdata;


import java.util.logging.Logger;

public class TestClassUsingJUL_5 {

    private static final Logger someLogger = Logger.getLogger(TestClassUsingJUL_5.class.getName());

    public void testSupplier() {
        someLogger.finest(() -> "the message");
    }
}
