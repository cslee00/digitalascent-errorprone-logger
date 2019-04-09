package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import org.immutables.value.Value;

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
