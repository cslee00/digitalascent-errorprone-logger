package com.digitalascent.errorprone.flogger.migrate.source.format;

public class MessageFormatConversionFailedException extends RuntimeException {
    public MessageFormatConversionFailedException(String message) {
        super(message);
    }
}
