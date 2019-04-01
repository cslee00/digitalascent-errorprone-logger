package com.digitalascent.errorprone.flogger.testdata;


import org.tinylog.Logger;

public class TestClassUsingTinyLog2_0 {

    private int x = 1;

    public void testLogLevels() {
        Logger.trace("test message");

        Logger.debug("test message");

        Logger.info("test message");

        Logger.warn("test message");

        Logger.error("test message");
    }


    public void testMessageFormat() {
        Logger.info("Test {} argument", "some");

        Logger.info("Test {} argument {}", "some", "other");

        Logger.info("Test {} argument {} 5%", "some", "other");

        Logger.info("Test \\{} argument {}", "other");

        Logger.info("Test \\\\{} argument {}", "some", "other");
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch( NullPointerException e ) {
            Logger.error(e,"The {} message", "exception");
        }
    }
    public void testOther() {
        Logger.info( "a" + 1 + "b");
        Logger.info( new Throwable(), "a" + 1 + "b {}", "argument");

        Logger.tag("someTag").debug("message");
    }
}
