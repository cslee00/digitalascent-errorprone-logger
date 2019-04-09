package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.ImmutableStyle;
import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatStyle;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import java.util.Map;

@ImmutableStyle
@Value.Immutable
public interface RefactoringConfiguration {
    LoggingApiConverter loggingApiConverter();

    Map<String, String> logLevelMappings();

    FloggerSuggestedFixGenerator floggerSuggestedFixGenerator();

    LoggerVariableDefinition loggerDefinition();

    @Nullable
    MessageFormatStyle messageFormatStyle();

    int lazyThresholdOrdinal();
}
