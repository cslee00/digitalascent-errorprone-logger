package com.digitalascent.errorprone.flogger;

import com.google.common.flogger.FluentLogger;

import java.util.Arrays;

public class FloggerOutput {
    private static final FluentLogger logger = FluentLogger.forEnclosingClass();

    public static void main( String[] args ) {
        Object[] array = new Object[] { "abc", "def", "ghi"};
        logger.atInfo().log("%s", array );
        logger.atInfo().log("%s", Arrays.toString(array) );
    }
}
