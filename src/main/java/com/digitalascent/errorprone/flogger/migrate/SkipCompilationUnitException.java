package com.digitalascent.errorprone.flogger.migrate;

public final class SkipCompilationUnitException extends RuntimeException {
    public SkipCompilationUnitException(String message) {
        super(message);
    }
}
