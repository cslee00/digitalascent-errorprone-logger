package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;

import java.util.Map;

public interface RefactoringConfiguration {
    LoggingApiConverter loggingApiConverter();

    Map<String, String> logLevelMappings();

    FloggerSuggestedFixGenerator floggerSuggestedFixGenerator();
}
