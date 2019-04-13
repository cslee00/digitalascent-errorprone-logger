package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.format.converter.CompositeMessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.converter.LazyMessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.converter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.ArraysToStringMessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.CompositeMessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.MessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.ToStringMessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableLoggerVariableDefinition;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableRefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.model.LoggerVariableDefinition;
import com.digitalascent.errorprone.flogger.migrate.model.RefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatStyle;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging.CommonsLoggingLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.jul.JULLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4JLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j.Log4jLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2LogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2.Log4j2LoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j.Slf4jLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog.TinyLogLoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2LogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2.TinyLog2LoggingApiSpecification;
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
        LoggingApiSpecification loggingApiSpecification = determineSourceApiConverter(sourceApi, floggerSuggestedFixGenerator, targetLogLevelFunction, messageFormatStyle);
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
                                                            FloggerSuggestedFixGenerator floggerSuggestedFixGenerator,
                                                            Function<String, TargetLogLevel> targetLogLevelFunction,
                                                            @Nullable MessageFormatStyle messageFormatStyle) {
        Map<String, LoggingApiSpecification> converterMap = buildConverterMap(targetLogLevelFunction, messageFormatStyle);
        LoggingApiSpecification converter = converterMap.get(sourceApi.toLowerCase().trim());
        if (converter == null) {
            throw new IllegalArgumentException("Unknown source API specified: " + sourceApi);
        }
        return converter;
    }

    private ImmutableMap<String, LoggingApiSpecification> buildConverterMap(Function<String, TargetLogLevel> targetLogLevelFunction,
                                                                            @Nullable MessageFormatStyle messageFormatStyle) {

        MessageFormatArgumentConverter messageFormatArgumentConverter = createMessageFormatArgumentConverter();
        MessageFormatArgumentReducer messageFormatArgumentReducer = createMessageFormatArgumentReducer();

        ImmutableMap.Builder<String, LoggingApiSpecification> converterMapBuilder = builder();

        LogMessageHandler logMessageHandler3 = new Slf4jLogMessageHandler(messageFormatArgumentConverter, messageFormatArgumentReducer);
        converterMapBuilder.put("slf4j", new Slf4jLoggingApiSpecification(targetLogLevelFunction, logMessageHandler3));

        LogMessageHandler logMessageHandler2 =  new Log4jLogMessageHandler(messageFormatArgumentConverter, messageFormatArgumentReducer);
        converterMapBuilder.put("log4j", new Log4JLoggingApiSpecification(targetLogLevelFunction, logMessageHandler2));

        converterMapBuilder.put("log4j2", createLog4j2LoggingApiConverter(targetLogLevelFunction,
                messageFormatStyle, messageFormatArgumentConverter, messageFormatArgumentReducer) );

        LogMessageHandler logMessageHandler = new CommonsLoggingLogMessageHandler(messageFormatArgumentConverter, messageFormatArgumentReducer);
        LoggingApiSpecification loggingApiSpecification = new CommonsLoggingLoggingApiSpecification(targetLogLevelFunction, logMessageHandler);
        converterMapBuilder.put("commons-logging", loggingApiSpecification);

        LogMessageHandler logMessageHandler4 = new TinyLogLogMessageHandler(messageFormatArgumentConverter, messageFormatArgumentReducer);
        converterMapBuilder.put("tinylog", new TinyLogLoggingApiSpecification(targetLogLevelFunction, logMessageHandler4));

        LogMessageHandler logMessageHandler5 =  new TinyLog2LogMessageHandler(messageFormatArgumentConverter, messageFormatArgumentReducer);
        converterMapBuilder.put("tinylog2", new TinyLog2LoggingApiSpecification(targetLogLevelFunction, logMessageHandler5));

        LogMessageHandler logMessageHandler1 = new JULLogMessageHandler(messageFormatArgumentConverter, messageFormatArgumentReducer);
        converterMapBuilder.put("jul", new JULLoggingApiSpecification(targetLogLevelFunction, logMessageHandler1));

        return converterMapBuilder.build();
    }

    private LoggingApiSpecification createLog4j2LoggingApiConverter(Function<String, TargetLogLevel> targetLogLevelFunction, @Nullable MessageFormatStyle messageFormatStyle, MessageFormatArgumentConverter messageFormatArgumentConverter, MessageFormatArgumentReducer messageFormatArgumentReducer) {
        LogMessageHandler logMessageHandler = new Log4j2LogMessageHandler(messageFormatStyle, messageFormatArgumentConverter, messageFormatArgumentReducer);
        return new Log4j2LoggingApiSpecification(targetLogLevelFunction, logMessageHandler);
    }

    private MessageFormatArgumentReducer createMessageFormatArgumentReducer() {
        ImmutableList.Builder<MessageFormatArgumentReducer> builder = ImmutableList.builder();
        builder.add(new ToStringMessageFormatArgumentReducer());
        builder.add(new ArraysToStringMessageFormatArgumentReducer());
        return new CompositeMessageFormatArgumentReducer(builder.build());
    }

    private MessageFormatArgumentConverter createMessageFormatArgumentConverter() {
        ImmutableList.Builder<MessageFormatArgumentConverter> builder = ImmutableList.builder();
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
