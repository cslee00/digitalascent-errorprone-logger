package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.source.format.argconverter.CompositeMessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.source.format.argconverter.LazyMessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.source.format.argconverter.Log4j2MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.source.format.argconverter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.source.format.argconverter.LambdaMessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.source.format.reducer.ArraysToStringMessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.source.format.reducer.CompositeMessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.source.format.reducer.MessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.source.format.reducer.ToStringMessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableLoggerVariableDefinition;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableRefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.model.LoggerVariableDefinition;
import com.digitalascent.errorprone.flogger.migrate.model.RefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.api.LogMessageFactory;
import com.digitalascent.errorprone.flogger.migrate.source.api.LoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatStyle;
import com.digitalascent.errorprone.flogger.migrate.source.api.commonslogging.CommonsLoggingLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.commonslogging.CommonsLoggingMessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.jul.JULLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.jul.JULMessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.log4j.Log4JLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.log4j.Log4jMessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.log4j2.Log4j2LoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.log4j2.Log4j2MessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.slf4j.Slf4jLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.slf4j.Slf4jMessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.tinylog.TinyLogLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.tinylog.TinyLogMessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.tinylog2.TinyLog2LoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.api.tinylog2.TinyLog2MessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.common.io.Resources;

import javax.annotation.Nullable;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static com.google.common.collect.ImmutableMap.builder;
import static com.google.common.collect.ImmutableMap.toImmutableMap;

@SuppressWarnings("UnstableApiUsage")
final class RefactoringConfigurationLoader {

    RefactoringConfiguration loadRefactoringConfiguration(String userProvidedPropertyPath, String sourceApi) {

        ImmutableRefactoringConfiguration.Builder builder = ImmutableRefactoringConfiguration.builder();
        Properties properties = loadProperties(userProvidedPropertyPath);

        LoggerVariableDefinition loggerVariableDefinition = readLoggerDefinition(properties);
        builder.loggerVariableDefinition(loggerVariableDefinition);

        FloggerSuggestedFixGenerator floggerSuggestedFixGenerator = new FloggerSuggestedFixGenerator(loggerVariableDefinition);
        builder.floggerSuggestedFixGenerator(floggerSuggestedFixGenerator);

        MessageFormatStyle messageFormatStyle = determineMessageFormatStyle(sourceApi, properties);

        Function<String, TargetLogLevel> targetLogLevelFunction = readLogLevelMappings(properties);
        LoggingApiSpecification loggingApiSpecification = determineSourceApiConverter(sourceApi, targetLogLevelFunction, messageFormatStyle);
        builder.loggingApiSpecification(loggingApiSpecification);

        // TODO
        builder.lazyThresholdOrdinal(-1);

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

    private LoggerVariableDefinition readLoggerDefinition(Properties properties) {
        ImmutableLoggerVariableDefinition.Builder builder = ImmutableLoggerVariableDefinition.builder();
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

    private LoggingApiSpecification determineSourceApiConverter(String sourceApi,
                                                                Function<String, TargetLogLevel> targetLogLevelFunction,
                                                                @Nullable MessageFormatStyle messageFormatStyle) {
        Map<String, LoggingApiSpecification> converterMap = createLoggingApiSpecifications(targetLogLevelFunction, messageFormatStyle);
        LoggingApiSpecification converter = converterMap.get(sourceApi.toLowerCase().trim());
        if (converter == null) {
            throw new IllegalArgumentException("Unknown source API specified: " + sourceApi);
        }
        return converter;
    }

    private ImmutableMap<String, LoggingApiSpecification> createLoggingApiSpecifications(
            Function<String, TargetLogLevel> targetLogLevelFunction,
            @Nullable MessageFormatStyle messageFormatStyle) {

        MessageFormatArgumentConverter messageFormatArgumentConverter = createMessageFormatArgumentConverter();
        MessageFormatArgumentReducer messageFormatArgumentReducer = createMessageFormatArgumentReducer();

        ImmutableMap.Builder<String, LoggingApiSpecification> converterMapBuilder = builder();

        converterMapBuilder.put("slf4j",
                new Slf4jLoggingApiSpecification(targetLogLevelFunction, new LogMessageFactory(
                        messageFormatArgumentConverter
                        , messageFormatArgumentReducer,
                        new Slf4jMessageFormatSpecification())));

        converterMapBuilder.put("log4j",
                new Log4JLoggingApiSpecification(targetLogLevelFunction, new LogMessageFactory(
                        messageFormatArgumentConverter,
                        messageFormatArgumentReducer,
                        new Log4jMessageFormatSpecification())));

        converterMapBuilder.put("log4j2",
                new Log4j2LoggingApiSpecification(targetLogLevelFunction, new LogMessageFactory(
                        messageFormatArgumentConverter,
                        messageFormatArgumentReducer,
                        new Log4j2MessageFormatSpecification(messageFormatStyle))));

        converterMapBuilder.put("commons-logging",
                new CommonsLoggingLoggingApiSpecification(targetLogLevelFunction, new LogMessageFactory(
                        messageFormatArgumentConverter,
                        messageFormatArgumentReducer,
                        new CommonsLoggingMessageFormatSpecification())));

        converterMapBuilder.put("tinylog",
                new TinyLogLoggingApiSpecification(targetLogLevelFunction, new LogMessageFactory(
                        messageFormatArgumentConverter,
                        messageFormatArgumentReducer,
                        new TinyLogMessageFormatSpecification())));

        converterMapBuilder.put("tinylog2",
                new TinyLog2LoggingApiSpecification(targetLogLevelFunction, new LogMessageFactory(
                        messageFormatArgumentConverter,
                        messageFormatArgumentReducer,
                        new TinyLog2MessageFormatSpecification())));

        converterMapBuilder.put("jul",
                new JULLoggingApiSpecification(targetLogLevelFunction, new LogMessageFactory(
                        messageFormatArgumentConverter,
                        messageFormatArgumentReducer,
                        new JULMessageFormatSpecification())));

        return converterMapBuilder.build();
    }

    private MessageFormatArgumentReducer createMessageFormatArgumentReducer() {
        ImmutableList.Builder<MessageFormatArgumentReducer> builder = ImmutableList.builder();
        builder.add(new ToStringMessageFormatArgumentReducer());
        builder.add(new ArraysToStringMessageFormatArgumentReducer());
        return new CompositeMessageFormatArgumentReducer(builder.build());
    }

    private MessageFormatArgumentConverter createMessageFormatArgumentConverter() {
        ImmutableList.Builder<MessageFormatArgumentConverter> builder = ImmutableList.builder();

        builder.add( new LambdaMessageFormatArgumentConverter());
        builder.add( new Log4j2MessageFormatArgumentConverter());

        // TODO - from configuration
        builder.add(new LazyMessageFormatArgumentConverter(-1));

        return new CompositeMessageFormatArgumentConverter(builder.build());
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
