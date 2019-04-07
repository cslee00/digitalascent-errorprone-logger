package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4JLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogLoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2LoggingApiConverter;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

@SuppressWarnings("UnstableApiUsage")
final class RefactoringConfigurationLoader {

    RefactoringConfiguration loadRefactoringConfiguration(String userProvidedPropertyPath, String sourceApi) {

        ImmutableRefactoringConfiguration.Builder builder = ImmutableRefactoringConfiguration.builder();

        Properties properties = loadProperties(userProvidedPropertyPath);

        LoggerDefinition loggerDefinition = readLoggerDefinition(properties);
        FloggerSuggestedFixGenerator floggerSuggestedFixGenerator = new FloggerSuggestedFixGenerator(loggerDefinition);
        builder.floggerSuggestedFixGenerator( floggerSuggestedFixGenerator );

        Function<String, TargetLogLevel> targetLogLevelFunction = new TargetLogLevelMapper();

        LoggingApiConverter converter = determineSourceApiConverter(sourceApi, floggerSuggestedFixGenerator, targetLogLevelFunction);
        builder.loggingApiConverter(converter);

        return builder.build();
    }

    private LoggerDefinition readLoggerDefinition(Properties properties) {
        ImmutableLoggerDefinition.Builder builder = ImmutableLoggerDefinition.builder();
        builder.name(properties.getProperty("logger.name"));
        builder.scope(properties.getProperty("logger.scope"));
        builder.modifiers(properties.getProperty("logger.modifiers"));
        builder.typeQualified(properties.getProperty("logger.type"));
        builder.type(properties.getProperty("logger.type.short"));
        builder.factoryMethod(properties.getProperty("logger.factory-method"));
        return builder.build();
    }

    private Properties loadProperties(String userProvidedPropertyPath) {
        URL url = Resources.getResource(getClass(), "logger-api-refactoring.properties");
        Properties baseProperties = load(Resources.asByteSource(url));
        Properties userProperties = new Properties();
        if (!Strings.isNullOrEmpty(userProvidedPropertyPath)) {
            userProperties = load(Files.asByteSource(new File(userProvidedPropertyPath)));
        }

        Properties finalProperties = new Properties();
        finalProperties.putAll(baseProperties);
        finalProperties.putAll(userProperties);

        return finalProperties;
    }

    private LoggingApiConverter determineSourceApiConverter(String sourceApi, FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        Map<String, LoggingApiConverter> converterMap = buildConverterMap(floggerSuggestedFixGenerator, targetLogLevelFunction);
        LoggingApiConverter converter = converterMap.get(sourceApi.toLowerCase().trim());
        if (converter == null) {
            throw new IllegalArgumentException("Unknown source API specified: " + sourceApi);
        }
        return converter;
    }

    private ImmutableMap<String, LoggingApiConverter> buildConverterMap(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        ImmutableMap.Builder<String, LoggingApiConverter> converterMapBuilder = ImmutableMap.builder();

        converterMapBuilder.put("slf4j", new Slf4JLoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("log4j", new Log4jLoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("log4j2", new Log4j2LoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("commons-logging", new CommonsLoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("tinylog", new TinyLogLoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("tinylog2", new TinyLog2LoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("jul", new JULLoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));

        return converterMapBuilder.build();
    }

    private Properties load(ByteSource byteSource) {
        final Properties props = new Properties();
        try (InputStream inputStream = byteSource.openBufferedStream()) {
            props.load(inputStream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return props;
    }
}
