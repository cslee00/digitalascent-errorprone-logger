package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.digitalascent.errorprone.flogger.migrate.source.api.LoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import org.immutables.value.Value;

@ImmutableStyle
@Value.Immutable
public interface RefactoringConfiguration {
    LoggingApiSpecification loggingApiSpecification();

    FloggerSuggestedFixGenerator floggerSuggestedFixGenerator();

    LoggerVariableDefinition loggerVariableDefinition();

    int lazyThresholdOrdinal();
}
