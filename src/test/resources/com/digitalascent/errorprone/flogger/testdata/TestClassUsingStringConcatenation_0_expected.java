package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;
import java.text.MessageFormat;
import java.util.Arrays;

public class TestClassUsingStringConcatenation_0 {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public void testStringConcatentation() {
        String x = "foo";
        logger.atInfo().log( "a%sb: %s", 1, x );
        // TODO [LoggerApiRefactoring] Unable to convert message format expression - not a string literal
        logger.atInfo().log( "a" + 1 + "b", "abc" );
    }
}
