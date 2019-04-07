package com.digitalascent.errorprone.flogger.testdata;

import com.google.common.flogger.FluentLogger;
import java.util.Arrays;

public class TestClassUsingMessageFormatArguments_0 {

    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public void testMessageFormatArguments() {
        logger.atInfo().log( "%s", new Object() );
        logger.atInfo().log( "%s", new Object() );
        logger.atInfo().log( "%s", new Object[] { "abc", "def", "ghi"} );
    }
}
