package com.digitalascent.errorprone.flogger.migrate;

public final class SkipCompilationUnitException extends RuntimeException {
    private static final long serialVersionUID = 42L;

    public SkipCompilationUnitException(String message) {
        super(message);
    }
}
