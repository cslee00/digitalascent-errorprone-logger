package com.digitalascent.errorprone.flogger.testdata;


import org.pmw.tinylog.Logger;

public class TestClassUsingTinyLog_0 {

    private int x = 1;

    public void testLogLevels() {
        Logger.trace("test message");

        Logger.debug("test message");

        Logger.info("test message");

        Logger.warn("test message");

        Logger.error("test message");
    }


    public void testMessageFormat() {
        Logger.info("1. Single parameter: {}","abc");
        Logger.info("2. Escaped formatting anchor: \\{}");
        Logger.info("3. Escaped anchor and single parameter: \\{} {}", "abc");
        Logger.info("4. Escaped anchors and single parameter: \\{} {} \\{}", "abc");
        Logger.info("5. Double-escaped anchor, single parameter: \\\\{}", "abc");
        Logger.info("6. Double-escaped anchor, no parameter: \\\\{}");
        Logger.info("7. Single parameter, double-escaped anchor: {} \\\\{}", "abc");
        Logger.info("8. Percent sign: 5% of {}", "abc");
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
    }
}
