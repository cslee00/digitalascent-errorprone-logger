package com.digitalascent.errorprone.flogger;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.tinylog.Logger;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;


class TinyLog2RefactoringTests extends AbstractLoggerApiFactoringTest {

    @ParameterizedTest
    @MethodSource("testDataProvider")
    void tinyLogTests(TestSpec testSpec) {
        executeTest("tinylog2", testSpec.testSource(), testSpec.expectedSource());
    }

    static Stream<TestSpec> testDataProvider() {

        Set<LogLevel> logLevels = ImmutableSet.of(
                new LogLevel("trace", "atFinest"),
                new LogLevel("debug", "atFine"),
                new LogLevel("info", "atInfo"),
                new LogLevel("warn", "atWarning"),
                new LogLevel("error", "atSevere")
        );

        Supplier<TestFixtures.TestSourceBuilder> floggerTestSourceBuilderSupplier = TestFixtures::builderWithFloggerLogger;
        Supplier<TestFixtures.TestSourceBuilder> sourceTestSourceBuilderSupplier = TestFixtures::builderWithoutLogger;

        TestSpecs testSpecs = new TestSpecs(sourceTestSourceBuilderSupplier, floggerTestSourceBuilderSupplier);
        for (LogLevel logLevel : logLevels) {
            testSpecs.add(logLevel,
                    "simple unformatted message",
                    (builder) ->
                            builder.addStatement("$T.$L($S)", Logger.class, logLevel.sourceLogLevel(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "supplier",
                    (builder) ->
                            builder.addStatement("$T.$L(() -> stringVar)", Logger.class, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, lazy(() -> stringVar))", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "integer message format",
                    (builder) ->
                            builder.addStatement("$T.$L(10)", Logger.class, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S,10)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "string concatenation",
                    (builder) ->
                            builder.addStatement("$T.$L($S + 1 + $S)", Logger.class, logLevel.sourceLogLevel(), "A", "B"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S,1)", logger, logLevel.targetLogLevel(), "A%sB")
            );

            testSpecs.add(logLevel,
                    "object as message",
                    (builder) ->
                            builder.addStatement("$T.$L(objectVar)", Logger.class, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "throwable as first argument",
                    (builder) ->
                            builder.addStatement("$T.$L(throwableVar)", Logger.class, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S)", logger, logLevel.targetLogLevel(), "Exception")
            );

            testSpecs.add(logLevel,
                    "throwable as first argument with message",
                    (builder) ->
                            builder.addStatement("$T.$L(throwableVar, $S)", Logger.class, logLevel.sourceLogLevel(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "throwable as first argument with message arguments",
                    (builder) ->
                            builder.addStatement("$T.$L(throwableVar, $S, objectVar)", Logger.class, logLevel.sourceLogLevel(), "test message {}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)", logger, logLevel.targetLogLevel(), "test message %s")
            );

            testSpecs.add(logLevel,
                    "unpack String.format",
                    (builder) ->
                            builder.addStatement("$T.$L(String.format($S,objectVar))",
                                    Logger.class, logLevel.sourceLogLevel(), "%s"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack String.format w/ Throwable",
                    (builder) ->
                            builder.addStatement("$T.$L(throwableVar, String.format($S,objectVar))",
                                    Logger.class, logLevel.sourceLogLevel(), "%s"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack Message.format",
                    (builder) ->
                            builder.addStatement("$T.$L($T.format($S,objectVar))",
                                    Logger.class, logLevel.sourceLogLevel(), MessageFormat.class, "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack Message.format w/ Throwable",
                    (builder) ->
                            builder.addStatement("$T.$L(throwableVar, $T.format($S,objectVar) )",
                                    Logger.class, logLevel.sourceLogLevel(), MessageFormat.class, "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)", logger, logLevel.targetLogLevel(), "%s")
            );
        }

        return testSpecs.testSpecs().stream();
    }
}