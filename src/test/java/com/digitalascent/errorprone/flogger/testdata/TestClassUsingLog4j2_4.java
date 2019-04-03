package com.digitalascent.errorprone.flogger.testdata;


import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class TestClassUsingLog4j2_4 {

    private final Logger someLogger = LogManager.getLogger("com.digitalascent.errorprone.flogger.testdata.TestClassUsingLog4j2_4");

    public void testLogLevels() {
        someLogger.trace("test message");
        someLogger.trace(DummyLog4J2Marker.INSTANCE, "test message");
        someLogger.log(Level.TRACE, "test message");
        someLogger.log(Level.TRACE, DummyLog4J2Marker.INSTANCE, "test message");
    }
}
