package com.digitalascent.errorprone.flogger.example;

import org.slf4j.*;

public class Slf4j {
    private Logger logger = LoggerFactory.getLogger( getClass() );

    public void someMethod() {
        logger.debug("Log message {}", "abc");
    }
}