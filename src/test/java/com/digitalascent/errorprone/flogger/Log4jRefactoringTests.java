package com.digitalascent.errorprone.flogger;

import com.google.common.collect.ImmutableSet;
import org.apache.log4j.Level;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;


class Log4jRefactoringTests extends AbstractLoggerApiFactoringTest {

    @ParameterizedTest
    @MethodSource("testDataProvider")
    void log4jTests(TestSpec testSpec) {
        executeTest("log4j", testSpec.testSource(), testSpec.expectedSource());
    }

    static Stream<TestSpec> testDataProvider() {

        Set<LogLevel> logLevels = ImmutableSet.of(
                new LogLevel("trace", "atFinest", true),
                new LogLevel("debug", "atFine", true),
                new LogLevel("info", "atInfo", true),
                new LogLevel("warn", "atWarning", false),
                new LogLevel("error", "atSevere", false),
                new LogLevel("fatal", "atSevere", false)
        );

        Set<String> levelsWithNoConditionals = ImmutableSet.of("warn","error","fatal");

        Supplier<TestFixtures.TestSourceBuilder> floggerTestSourceBuilderSupplier = TestFixtures::builderWithFloggerLogger;
        Supplier<TestFixtures.TestSourceBuilder> sourceTestSourceBuilderSupplier = TestFixtures::builderWithLog4JLogger;

        TestSpecs testSpecs = new TestSpecs(sourceTestSourceBuilderSupplier, floggerTestSourceBuilderSupplier);
        for (LogLevel logLevel : logLevels) {

            if(levelSupportsConditional(levelsWithNoConditionals, logLevel)) {
                buildConditionalTestSpecs(testSpecs, logLevel);
            }

            testSpecs.add(logLevel,
                    "simple unformatted message",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "log(Level, ...) simple message",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$L,$S)", logger, Level.class, logLevel.sourceLogLevelUpperCase(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "log simple message with throwable",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$L,$S, throwableVar)", logger, Level.class, logLevel.sourceLogLevelUpperCase(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "integer message format",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(10)", logger, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S,10)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "string concatenation",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S + 1 + $S)", logger, logLevel.sourceLogLevel(), "A", "B"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S,1)", logger, logLevel.targetLogLevel(), "A%sB")
            );

            testSpecs.add(logLevel,
                    "object as message",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(objectVar)", logger, logLevel.sourceLogLevel()),
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
                            builder.addStatement("$N.$L(objectVar, throwableVar)", logger, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "throwable as only argument",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(throwableVar)", logger, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S)", logger, logLevel.targetLogLevel(), "Exception")
            );

            testSpecs.add(logLevel,
                    "unpack String.format",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(String.format($S,objectVar))", logger, logLevel.sourceLogLevel(), "%s"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            if( logLevel.lazyArgs()) {
                testSpecs.add(logLevel,
                        "expensive argument",
                        (builder, logger) ->
                                builder.addStatement("$N.$L(String.format($S,dummyMethod()))", logger, logLevel.sourceLogLevel(), "%s"),
                        (builder, logger) ->
                                builder.addStatement("$N.$L().log($S, lazy(() -> dummyMethod()))", logger, logLevel.targetLogLevel(), "%s")
                );
            } else {
                testSpecs.add(logLevel,
                        "expensive argument",
                        (builder, logger) ->
                                builder.addStatement("$N.$L(String.format($S,dummyMethod()))", logger, logLevel.sourceLogLevel(), "%s"),
                        (builder, logger) ->
                                builder.addStatement("$N.$L().log($S, dummyMethod())", logger, logLevel.targetLogLevel(), "%s")
                );
            }

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

    private static void buildConditionalTestSpecs(TestSpecs testSpecs, LogLevel logLevel) {
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
    }

    private static boolean levelSupportsConditional(Set<String> levelsWithNoConditionals, LogLevel logLevel) {
        return !levelsWithNoConditionals.contains(logLevel.sourceLogLevel());
    }
}