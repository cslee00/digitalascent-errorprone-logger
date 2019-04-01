package com.digitalascent.errorprone.flogger;

import org.immutables.value.Value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
@Value.Style(
        visibility = Value.Style.ImplementationVisibility.SAME,
        builderVisibility =  Value.Style.BuilderVisibility.SAME,
        depluralize = true,
        strictBuilder = true,
        defaults = @Value.Immutable(copy = false))
public @interface ImmutableStyle {
}
