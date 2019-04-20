package com.digitalascent.errorprone.flogger.migrate;

public final class SkipLogMethodException extends RuntimeException {
    private static final long serialVersionUID = 42L;

    public SkipLogMethodException(String message) {
        super(message);
    }
}
