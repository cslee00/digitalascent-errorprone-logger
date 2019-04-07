package com.digitalascent.errorprone.flogger.migrate;

import java.util.Map;

public interface RefactoringConfiguration {
    LoggingApiConverter loggingApiConverter();

    Map<String, String> logLevelMappings();

    FloggerSuggestedFixGenerator floggerSuggestedFixGenerator();

    LoggerDefinition loggerDefinition();
}
