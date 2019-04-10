package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import org.immutables.value.Value;

/**
 * Definition of a Flogger logger member variable, suitable for generating the code representing that variable
 */
@ImmutableStyle
@Value.Immutable
public interface LoggerVariableDefinition {
    String name();
    String scope();
    String modifiers();
    String typeQualified();
    String type();
    String factoryMethod();
}
