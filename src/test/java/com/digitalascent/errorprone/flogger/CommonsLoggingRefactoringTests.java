package com.digitalascent.errorprone.flogger;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.MessageFormat;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;


class CommonsLoggingRefactoringTests extends AbstractLoggerApiFactoringTest {

    @ParameterizedTest
    @MethodSource("testDataProvider")
    void commonsLoggingTests(TestSpec testSpec) {
        executeTest("commons-logging", testSpec.testSource(), testSpec.expectedSource());
    }

    static Stream<TestSpec> testDataProvider() {

        Set<LogLevel> logLevels = ImmutableSet.of(
                new LogLevel("trace", "atFinest"),
                new LogLevel("debug", "atFine"),
                new LogLevel("info", "atInfo"),
                new LogLevel("warn", "atWarning"),
                new LogLevel("error", "atSevere"),
                new LogLevel("fatal", "atSevere")
        );

        Supplier<TestFixtures.TestSourceBuilder> floggerTestSourceBuilderSupplier = TestFixtures::builderWithFloggerLogger;
        Supplier<TestFixtures.TestSourceBuilder> sourceTestSourceBuilderSupplier = TestFixtures::builderWithCommonsLoggingLogger;

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
                    "collapse conditional no statements",
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.is$LEnabled() )", logger, logLevel.sourceLogLevelTitleCase());
                        builder.endControlFlow();
                    },
                    (builder, logger) -> {}
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