package com.digitalascent.errorprone.flogger.migrate;

public final class SkipLogMethodException extends RuntimeException {
    public SkipLogMethodException(String message) {
        super(message);
    }
}
