package com.digitalascent.errorprone.flogger.migrate.source.api.slf4j;


import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

public class Log4jOutput {
    public static void main(String[] args ) {
        Logger logger = LogManager.getLogger(Log4jOutput.class);

        logger.info(new Throwable());
        try {
            String s = null;
            s.trim();
        } catch( NullPointerException e ) {
            logger.info("Exception", e);
            logger.info(e);
        }
    }
}
