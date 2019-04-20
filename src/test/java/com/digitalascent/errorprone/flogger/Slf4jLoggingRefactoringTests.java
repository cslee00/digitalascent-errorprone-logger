package com.digitalascent.errorprone.flogger;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;


class Slf4jLoggingRefactoringTests extends AbstractLoggerApiFactoringTest {

    @ParameterizedTest
    @MethodSource("testDataProvider")
    void slf4jTests(TestSpec testSpec) {
        executeTest("slf4j", testSpec.testSource(), testSpec.expectedSource());
    }

    static Stream<TestSpec> testDataProvider() {

        Set<LogLevel> logLevels = ImmutableSet.of(
                new LogLevel("trace", "atFinest", true),
                new LogLevel("debug", "atFine", true),
                new LogLevel("info", "atInfo", true),
                new LogLevel("warn", "atWarning", false),
                new LogLevel("error", "atSevere", false)
        );

        Supplier<TestFixtures.TestSourceBuilder> floggerTestSourceBuilderSupplier = TestFixtures::builderWithFloggerLogger;
        Supplier<TestFixtures.TestSourceBuilder> sourceTestSourceBuilderSupplier = TestFixtures::builderWithSLF4JLogger;

        TestSpecs testSpecs = new TestSpecs(sourceTestSourceBuilderSupplier, floggerTestSourceBuilderSupplier);
        for (LogLevel logLevel : logLevels) {
            testSpecs.add(logLevel,
                    "simple unformatted message",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "simple unformatted message with marker",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($T.$L, $S)", logger, logLevel.sourceLogLevel(), DummySlf4JMarker.class, "INSTANCE", "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );


            testSpecs.add(logLevel,
                    "collapse conditional no statements",
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.is$LEnabled() )", logger, logLevel.sourceLogLevelTitleCase());
                        builder.endControlFlow();
                    },
                    (builder, logger) -> {
                    }
            );

            testSpecs.add(logLevel,
                    "collapse conditional one statement",
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.is$LEnabled() )", logger, logLevel.sourceLogLevelTitleCase());
                        builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message");
                        builder.endControlFlow();
                    },
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "collapse conditional one statement with marker",
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.is$LEnabled($T.INSTANCE) )", logger, logLevel.sourceLogLevelTitleCase(), DummySlf4JMarker.class);
                        builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message");
                        builder.endControlFlow();
                    },
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "collapse conditional two statements",
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.is$LEnabled() )", logger, logLevel.sourceLogLevelTitleCase());
                        builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message");
                        builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message");
                        builder.endControlFlow();
                    },
                    (builder, logger) -> {
                        builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message");
                        builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message");
                    }
            );

            testSpecs.add(logLevel,
                    "migrate compound conditional",
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.is$LEnabled() )", logger, logLevel.sourceLogLevelTitleCase());
                        builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message");
                        builder.addStatement("dummyMethod()");
                        builder.endControlFlow();
                    },
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.$L().isEnabled() )", logger, logLevel.targetLogLevel());
                        builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message");
                        builder.addStatement("dummyMethod()");
                        builder.endControlFlow();
                    }
            );

            testSpecs.add(logLevel,
                    "single argument",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S,10)", logger, logLevel.sourceLogLevel(), "{}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S,10)", logger, logLevel.targetLogLevel(), "%s")
            );

            if( logLevel.lazyArgs()) {
                testSpecs.add(logLevel,
                        "expensive argument",
                        (builder, logger) ->
                                builder.addStatement("$N.$L($S,dummyMethod())", logger, logLevel.sourceLogLevel(), "{}"),
                        (builder, logger) ->
                                builder.addStatement("$N.$L().log($S,lazy(() -> dummyMethod()))", logger, logLevel.targetLogLevel(), "%s")
                );
            } else {
                testSpecs.add(logLevel,
                        "expensive argument",
                        (builder, logger) ->
                                builder.addStatement("$N.$L($S,dummyMethod())", logger, logLevel.sourceLogLevel(), "{}"),
                        (builder, logger) ->
                                builder.addStatement("$N.$L().log($S,dummyMethod())", logger, logLevel.targetLogLevel(), "%s")
                );
            }

            testSpecs.add(logLevel,
                    "single argument w/ Object[]",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S, new Object[] { 10, 20, 30 })", logger, logLevel.sourceLogLevel(), "{} {} {}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, 10, 20, 30)", logger, logLevel.targetLogLevel(), "%s %s %s")
            );

            testSpecs.add(logLevel,
                    "single argument w/ Object[] w/ Throwable",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S, new Object[] { 10, 20, 30, throwableVar })", logger, logLevel.sourceLogLevel(), "{} {} {}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, 10, 20, 30)", logger, logLevel.targetLogLevel(), "%s %s %s")
            );

            testSpecs.add(logLevel,
                    "escaped format anchor no parameters",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "\\{}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "\\{}")
            );

            testSpecs.add(logLevel,
                    "escaped format anchor one parameter",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S, objectVar)", logger, logLevel.sourceLogLevel(), "\\{} {}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "{} %s")
            );

            testSpecs.add(logLevel,
                    "double-escaped format anchor no parameters",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "\\\\{}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "\\\\{}")
            );

            testSpecs.add(logLevel,
                    "double-escaped format anchor one parameter",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S, objectVar)", logger, logLevel.sourceLogLevel(), "\\\\{} {}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "\\%s %s")
            );

            testSpecs.add(logLevel,
                    "percent sign escaped",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S, objectVar)", logger, logLevel.sourceLogLevel(), "{} 5% {}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s 5%% %s")
            );

            testSpecs.add(logLevel,
                    "string concatenation",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S + objectVar + $S)", logger, logLevel.sourceLogLevel(), "A", "B"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S,objectVar)", logger, logLevel.targetLogLevel(), "A%sB")
            );

            testSpecs.add(logLevel,
                    "string concatenation one argument",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S + objectVar + $S, objectVar)", logger, logLevel.sourceLogLevel(), "A", "B"),
                    (builder, logger) -> builder
                            .addComment("TODO [LoggerApiRefactoring] Unable to convert message format expression - not a string")
                            .addComment("literal")
                            .addStatement("$N.$L().log($S + objectVar + $S, objectVar)", logger, logLevel.targetLogLevel(), "A", "B")

            );

            testSpecs.add(logLevel,
                    "object variable",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S,objectVar)", logger, logLevel.sourceLogLevel(), "{}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "throwable as last argument with simple message",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S, throwableVar)", logger, logLevel.sourceLogLevel(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "throwable as last argument with object message",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S,objectVar, throwableVar)", logger, logLevel.sourceLogLevel(), "{}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack String.format",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(String.format($S,objectVar))", logger, logLevel.sourceLogLevel(), "%s"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack String.format w/ Throwable",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(String.format($S,objectVar), throwableVar)", logger, logLevel.sourceLogLevel(), "%s"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack Message.format",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($T.format($S,objectVar))", logger, logLevel.sourceLogLevel(), MessageFormat.class, "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack Message.format w/ Throwable",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($T.format($S,objectVar), throwableVar)", logger, logLevel.sourceLogLevel(), MessageFormat.class, "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );
        }

        return testSpecs.testSpecs().stream();
    }
}