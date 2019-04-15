package com.digitalascent.errorprone.flogger.example;

import org.slf4j.*;
import java.text.*;

public class Slf4j {
    private Logger logger = LoggerFactory.getLogger( getClass() );

    public void someMethod() {
        logger.debug("Log message {}", "abc");
        logger.debug( String.format("%s", "abc"));
        logger.debug("Log message {}", new Object[] { "abc"});
        logger.debug("Log message", new Throwable());
        logger.debug(MessageFormat.format("{0}", "abc"));
        if( logger.isDebugEnabled() ) {
            logger.debug("Some message");
        }
    }
}