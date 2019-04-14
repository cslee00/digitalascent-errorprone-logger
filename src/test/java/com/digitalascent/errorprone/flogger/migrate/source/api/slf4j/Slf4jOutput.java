package com.digitalascent.errorprone.flogger.migrate.source.api.slf4j;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Slf4jOutput {
    public static void main(String[] args ) {
        Logger logger = LoggerFactory.getLogger(Slf4jOutput.class);

        logger.info("1. Single parameter: {}","abc");
        logger.info("2. Escaped formatting anchor: \\{}");
        logger.info("3. Escaped anchor and single parameter: \\{} {}", "abc");
        logger.info("4. Escaped anchors and single parameter: \\{} {} \\{}", "abc");
        logger.info("5. Double-escaped anchor, single parameter: \\\\{}", "abc");
        logger.info("6. Double-escaped anchor, no parameter: \\\\{}");
        logger.info("7. Single parameter, double-escaped anchor: {} \\\\{}", "abc");
        logger.info("9. Explicit Object[] {} {} {}", new Object[] { "abc", "def", "ghi"});
    }
}
