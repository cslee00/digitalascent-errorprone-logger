package com.digitalascent.errorprone.flogger.testdata;



public class TestClassUsingSlf4j_5 {

    // TODO [LoggerApiRefactoring] Unable to migrate logger variable
    private final Logger logger = LoggerFactory.getLogger("some random logger");

    public void testLogLevels() {
        logger.atFinest().log( "test message" );
        logger.atFinest().log( "test message" );
    }
}