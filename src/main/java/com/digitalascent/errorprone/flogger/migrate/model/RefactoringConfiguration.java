package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import org.immutables.value.Value;

@ImmutableStyle
@Value.Immutable
public interface RefactoringConfiguration {
    LoggingApiConverter loggingApiConverter();

    FloggerSuggestedFixGenerator floggerSuggestedFixGenerator();

    LoggerVariableDefinition loggerDefinition();

    int lazyThresholdOrdinal();
}
