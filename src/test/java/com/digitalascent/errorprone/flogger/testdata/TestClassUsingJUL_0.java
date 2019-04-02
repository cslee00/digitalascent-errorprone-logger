package com.digitalascent.errorprone.flogger.testdata;


import java.util.logging.Level;
import java.util.logging.Logger;

public class TestClassUsingJUL_0 {
    private int x = 1;

    public void testLogLevels() {
        someLogger.finest("test message");
        someLogger.log(Level.FINEST, "test message");

        someLogger.finer("test message");
        someLogger.log(Level.FINER, "test message");

        someLogger.fine("test message");
        someLogger.log(Level.FINE, "test message");

        someLogger.config("test message");
        someLogger.log(Level.CONFIG, "test message");

        someLogger.info("test message");
        someLogger.log(Level.INFO, "test message");

        someLogger.warning("test message");
        someLogger.log(Level.WARNING, "test message");

        someLogger.severe("test message");
        someLogger.log(Level.SEVERE, "test message");

        someLogger.log(CustomJULLevel.LEVEL_1, "test message");
        someLogger.log(CustomJULLevel.LEVEL_2, "test message");
        someLogger.log(CustomJULLevel.LEVEL_3, "test message");
    }

    public void testEnabled() {
        someLogger.isLoggable(Level.FINEST);
        someLogger.isLoggable(Level.FINER);
        someLogger.isLoggable(Level.FINE);
        someLogger.isLoggable(Level.CONFIG);
        someLogger.isLoggable(Level.INFO);
        someLogger.isLoggable(Level.WARNING);
        someLogger.isLoggable(Level.SEVERE);

        someLogger.isLoggable(CustomJULLevel.LEVEL_1);
        someLogger.isLoggable(CustomJULLevel.LEVEL_2);
        someLogger.isLoggable(CustomJULLevel.LEVEL_3);
    }

    public void testMessageFormat() {
        someLogger.log(Level.INFO, "1. Single parameter: {0}", "abc");
        someLogger.log(Level.INFO, "2. Escaped formatting anchor: \\{0}");
        someLogger.log(Level.INFO, "3. Escaped anchor and single parameter: \\{0} {0}", "abc");
        someLogger.log(Level.INFO, "4. Escaped anchors and single parameter: \\{0} {0} \\{0}", "abc");
        someLogger.log(Level.INFO, "5. Double-escaped anchor, single parameter: \\\\{0}", "abc");
        someLogger.log(Level.INFO, "6. Double-escaped anchor, no parameter: \\\\{0}");
        someLogger.log(Level.INFO, "7. Single parameter, double-escaped anchor: {0} \\\\{0}", "abc");
        someLogger.log(Level.INFO, "8. Multi parameters: {1} {0} {0}", new Object[] {"abc", "def"});
        someLogger.log(Level.INFO, "9. Multi parameters 2: {9} {0} {0}", new Object[] {"abc", "def", "ghi"});
    }

    public void testException() {
        try {
            String s = null;
            s.trim();
        } catch (NullPointerException e) {
            someLogger.log(Level.INFO, "Exception!!!", e);
        }
    }

    public void testOther() {
        someLogger.info("a" + 1 + "b");
    }

    private final Logger someLogger = Logger.getLogger(getClass().getName());
}
