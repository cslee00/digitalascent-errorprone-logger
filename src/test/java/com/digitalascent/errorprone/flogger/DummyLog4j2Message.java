package com.digitalascent.errorprone.flogger;

import org.apache.logging.log4j.message.Message;

public class DummyLog4j2Message implements Message {
    @Override
    public String getFormattedMessage() {
        return "abc";
    }

    @Override
    public String getFormat() {
        return "abc";
    }

    @Override
    public Object[] getParameters() {
        return new Object[0];
    }

    @Override
    public Throwable getThrowable() {
        return null;
    }
}
