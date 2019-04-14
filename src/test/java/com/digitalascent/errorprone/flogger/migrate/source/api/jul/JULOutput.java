package com.digitalascent.errorprone.flogger.migrate.source.api.jul;


import java.util.logging.Level;
import java.util.logging.Logger;

public class JULOutput {

    public static void main( String[] args ) {
        Logger logger = Logger.getLogger(JULOutput.class.getName());

        logger.log(Level.INFO, "1. Single parameter: {0}","abc");
        logger.log(Level.INFO, "2. Escaped formatting anchor: \\{0}");
        logger.log(Level.INFO, "3. Escaped anchor and single parameter: \\{0} {0}", "abc");
        logger.log(Level.INFO, "4. Escaped anchors and single parameter: \\{0} {0} \\{0}", "abc");
        logger.log(Level.INFO, "5. Double-escaped anchor, single parameter: \\\\{0}", "abc");
        logger.log(Level.INFO, "6. Double-escaped anchor, no parameter: \\\\{0}");
        logger.log(Level.INFO, "7. Single parameter, double-escaped anchor: {0} \\\\{0}", "abc");
    }
}
