package com.digitalascent.errorprone.flogger.migrate.source.api.log4j2;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


public class Log4j2Output {

    public static void main( String[] args ) {
        Logger logger = LogManager.getLogger();

        logger.info("1. Single parameter: {}","abc");
        logger.info("2. Escaped formatting anchor: \\{}");
        logger.info("3. Escaped anchor and single parameter: \\{} {}", "abc");
        logger.info("4. Escaped anchors and single parameter: \\{} {} \\{}", "abc");
        logger.info("5. Double-escaped anchor, single parameter: \\\\{}", "abc");
        logger.info("6. Double-escaped anchor, no parameter: \\\\{}");
        logger.info("7. Single parameter, double-escaped anchor: {} \\\\{}", "abc");
    }
}
