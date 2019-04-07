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

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Target;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMap.*;

@SuppressWarnings("UnstableApiUsage")
final class RefactoringConfigurationLoader {

    RefactoringConfiguration loadRefactoringConfiguration(String userProvidedPropertyPath, String sourceApi) {

        ImmutableRefactoringConfiguration.Builder builder = ImmutableRefactoringConfiguration.builder();
        Properties properties = loadProperties(userProvidedPropertyPath);

        LoggerDefinition loggerDefinition = readLoggerDefinition(properties);
        builder.loggerDefinition(loggerDefinition);

        FloggerSuggestedFixGenerator floggerSuggestedFixGenerator = new FloggerSuggestedFixGenerator(loggerDefinition);
        builder.floggerSuggestedFixGenerator(floggerSuggestedFixGenerator);

        MessageFormatStyle messageFormatStyle = determineMessageFormatStyle(sourceApi, properties);
        builder.messageFormatStyle(messageFormatStyle);

        Function<String, TargetLogLevel> targetLogLevelFunction = readLogLevelMappings(properties);
        LoggingApiConverter converter = determineSourceApiConverter(sourceApi, floggerSuggestedFixGenerator, targetLogLevelFunction, messageFormatStyle);
        builder.loggingApiConverter(converter);

        return builder.build();
    }

    @Nullable
    private MessageFormatStyle determineMessageFormatStyle(String sourceApi, Properties properties) {
        if ("log4j2".equals(sourceApi)) {
            String log4j2DefaultMessageFormat = properties.getProperty("log4j2.default-message-format");
            switch (log4j2DefaultMessageFormat) {
                case "brace":
                    return MessageFormatStyle.LOG4J2_BRACE;
                case "printf":
                    return MessageFormatStyle.PRINTF;
                default:
                    throw new AssertionError("Unknown Log4J2 message format: " + log4j2DefaultMessageFormat);
            }
        }
        return null;
    }

    private Function<String, TargetLogLevel> readLogLevelMappings(Properties properties) {
        Map<String, String> propMap = properties.entrySet().stream().collect(
                toImmutableMap(e -> (String) e.getKey(), e -> (String) e.getValue()));

        Map<String, TargetLogLevel> logLevelMap = propMap.entrySet().stream()
                .filter(e -> e.getKey().startsWith("level.") && e.getKey().endsWith(".mapping"))
                .collect(toImmutableMap(
                        e -> e.getKey().replace("level.", "").replace(".mapping", ""),
                        e -> new TargetLogLevel(e.getValue())));
        return new TargetLogLevelMapper(logLevelMap);
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

    private LoggingApiConverter determineSourceApiConverter(String sourceApi,
                                                            FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                                            Function<String, TargetLogLevel> targetLogLevelFunction,
                                                            @Nullable MessageFormatStyle messageFormatStyle) {
        Map<String, LoggingApiConverter> converterMap = buildConverterMap(floggerSuggestedFixGenerator, targetLogLevelFunction, messageFormatStyle);
        LoggingApiConverter converter = converterMap.get(sourceApi.toLowerCase().trim());
        if (converter == null) {
            throw new IllegalArgumentException("Unknown source API specified: " + sourceApi);
        }
        return converter;
    }

    private ImmutableMap<String, LoggingApiConverter> buildConverterMap(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                                                        Function<String, TargetLogLevel> targetLogLevelFunction,
                                                                        @Nullable MessageFormatStyle messageFormatStyle) {
        ImmutableMap.Builder<String, LoggingApiConverter> converterMapBuilder = builder();

        converterMapBuilder.put("slf4j", new Slf4JLoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("log4j", new Log4jLoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction));
        converterMapBuilder.put("log4j2", new Log4j2LoggingApiConverter(floggerSuggestedFixGenerator, targetLogLevelFunction, messageFormatStyle));
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
