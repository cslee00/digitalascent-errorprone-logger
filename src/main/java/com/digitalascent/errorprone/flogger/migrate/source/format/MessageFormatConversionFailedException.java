package com.digitalascent.errorprone.flogger.migrate.source.format;

public class MessageFormatConversionFailedException extends RuntimeException {
    private static final long serialVersionUID = 42L;

    public MessageFormatConversionFailedException(String message) {
        super(message);
    }
}
