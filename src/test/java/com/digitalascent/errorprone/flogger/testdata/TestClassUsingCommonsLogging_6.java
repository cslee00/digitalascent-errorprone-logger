package com.digitalascent.errorprone.flogger.testdata;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class TestClassUsingCommonsLogging_6 {
    private final Log someLogger = LogFactory.getLog(getClass());

    public void testConditionalLogging() {
        if( someLogger.isTraceEnabled() ) {
            someLogger.trace("test message");
        }

        if( someLogger.isDebugEnabled() ) {
            someLogger.debug("test message");
        }

        if( someLogger.isInfoEnabled() ) {
            someLogger.info("test message");
        }
        if( someLogger.isWarnEnabled()) {
            someLogger.warn("test message");
        }
        if( someLogger.isErrorEnabled()) {
            someLogger.error("test message");
        }
        if( someLogger.isFatalEnabled()) {
            someLogger.fatal("test message");
        }
    }

    public void testConditionalLoggingMultiple() {
        if( someLogger.isTraceEnabled() ) {
            someLogger.trace("test message");
            someLogger.trace("test message");
        }

        if( someLogger.isDebugEnabled() ) {
            someLogger.debug("test message");
            someLogger.debug("test message");
        }

        if( someLogger.isInfoEnabled() ) {
            someLogger.info("test message");
            someLogger.info("test message");
        }
        if( someLogger.isWarnEnabled()) {
            someLogger.warn("test message");
            someLogger.warn("test message");
        }
        if( someLogger.isErrorEnabled()) {
            someLogger.error("test message");
            someLogger.error("test message");
        }
        if( someLogger.isFatalEnabled()) {
            someLogger.fatal("test message");
            someLogger.fatal("test message");
        }
    }

    public void testConditionalLoggingMultipleEmpty() {
        if( someLogger.isTraceEnabled() ) {
        }
        if( someLogger.isDebugEnabled() ) {
        }
        if( someLogger.isInfoEnabled() ) {
        }
        if( someLogger.isWarnEnabled()) {
        }
        if( someLogger.isErrorEnabled()) {
        }
        if( someLogger.isFatalEnabled()) {
        }
    }

    public void testConditionalLoggingMultiple2() {
        if( someLogger.isTraceEnabled() ) {
            someLogger.trace("test message");
            someOtherMethod();
        }

        if( someLogger.isDebugEnabled() ) {
            someLogger.debug("test message");
            someOtherMethod();
        }

        if( someLogger.isInfoEnabled() ) {
            someLogger.info("test message");
            someOtherMethod();
        }
        if( someLogger.isWarnEnabled()) {
            someLogger.warn("test message");
            someOtherMethod();
        }
        if( someLogger.isErrorEnabled()) {
            someLogger.error("test message");
            someOtherMethod();
        }
        if( someLogger.isFatalEnabled()) {
            someLogger.fatal("test message");
            someOtherMethod();
        }
    }

    private void someOtherMethod() {

    }
}
