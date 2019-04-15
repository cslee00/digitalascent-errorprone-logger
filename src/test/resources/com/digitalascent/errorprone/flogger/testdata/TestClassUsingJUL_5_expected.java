package com.digitalascent.errorprone.flogger.testdata;


import static com.google.common.flogger.LazyArgs.lazy;

import com.google.common.flogger.FluentLogger;

public class TestClassUsingJUL_5 {

    private static final FluentLogger someLogger = FluentLogger.forEnclosingClass();

    public void testSupplier() {
        someLogger.atFinest().log( "%s", lazy(() -> "the message") );
    }
}
