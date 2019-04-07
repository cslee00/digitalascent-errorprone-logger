package com.digitalascent.errorprone.flogger.migrate;

public interface LoggerDefinition {
    String name();
    String scope();
    String modifiers();
    String typeQualified();
    String type();
    String factoryMethod();
}
