package com.digitalascent.errorprone.flogger.migrate.sourceapi;

public class MessageFormatConversionFailedException extends RuntimeException {
    public MessageFormatConversionFailedException(String message) {
        super(message);
    }
}
