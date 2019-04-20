package com.digitalascent.errorprone.flogger;

import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.stream.Stream;


class JULRefactoringTests extends AbstractLoggerApiFactoringTest {

    @ParameterizedTest
    @MethodSource("testDataProvider")
    void julTests(TestSpec testSpec) {
        executeTest("jul", testSpec.testSource(), testSpec.expectedSource());
    }

    static Stream<TestSpec> testDataProvider() {

        Set<LogLevel> logLevels = ImmutableSet.of(
                new LogLevel("finest", "atFinest", true),
                new LogLevel("finer", "atFiner", true),
                new LogLevel("fine", "atFine", true),
                new LogLevel("config", "atConfig", true),
                new LogLevel("info", "atInfo", true),
                new LogLevel("warning", "atWarning", false),
                new LogLevel("severe", "atSevere", false)
        );

        LogLevel finer = new LogLevel("finer", "atFiner", true);

        Supplier<TestFixtures.TestSourceBuilder> floggerTestSourceBuilderSupplier = TestFixtures::builderWithFloggerLogger;
        Supplier<TestFixtures.TestSourceBuilder> sourceTestSourceBuilderSupplier = TestFixtures::builderWithJULLogger;

        TestSpecs testSpecs = new TestSpecs(sourceTestSourceBuilderSupplier, floggerTestSourceBuilderSupplier);
        buildLogLevelTestSpecs(logLevels, testSpecs);
        buildEnteringTestSpecs(finer, testSpecs);
        buildExitingTestSpecs(finer, testSpecs);
        buildThrowingTestSpecs(finer, testSpecs);

        return testSpecs.testSpecs().stream();
    }

    private static void buildThrowingTestSpecs(LogLevel finer, TestSpecs testSpecs) {
        testSpecs.add(finer,
                "throwing",
                (builder, logger) ->
                        builder.addStatement("$N.throwing($S,$S, throwableVar)", logger, "abc", "def"),
                (builder, logger) ->
                        builder.addStatement("$N.atFiner().withCause(throwableVar).log($S)", logger, "THROW")
        );
    }

    private static void buildExitingTestSpecs(LogLevel finer, TestSpecs testSpecs) {
        testSpecs.add(finer,
                "entering multiple params",
                (builder, logger) ->
                        builder.addStatement("$N.entering($S,$S, new Object[] { objectVar, objectVar })", logger, "abc", "def"),
                (builder, logger) ->
                        builder.addStatement("$N.atFiner().log($S, objectVar, objectVar)", logger, "ENTRY %s %s")
        );

        testSpecs.add(finer,
                "exiting no params",
                (builder, logger) ->
                        builder.addStatement("$N.exiting($S,$S)", logger, "abc", "def"),
                (builder, logger) ->
                        builder.addStatement("$N.atFiner().log($S)", logger, "RETURN")
        );

        testSpecs.add(finer,
                "exiting one params",
                (builder, logger) ->
                        builder.addStatement("$N.exiting($S,$S, objectVar)", logger, "abc", "def"),
                (builder, logger) ->
                        builder.addStatement("$N.atFiner().log($S, objectVar)", logger, "RETURN %s")
        );
    }

    private static void buildEnteringTestSpecs(LogLevel finer, TestSpecs testSpecs) {
        testSpecs.add(finer,
                "entering no params",
                (builder, logger) ->
                        builder.addStatement("$N.entering($S,$S)", logger, "abc", "def"),
                (builder, logger) ->
                        builder.addStatement("$N.atFiner().log($S)", logger, "ENTRY")
        );

        testSpecs.add(finer,
                "entering one params",
                (builder, logger) ->
                        builder.addStatement("$N.entering($S,$S, objectVar)", logger, "abc", "def"),
                (builder, logger) ->
                        builder.addStatement("$N.atFiner().log($S, objectVar)", logger, "ENTRY %s")
        );
    }

    private static void buildLogLevelTestSpecs(Set<LogLevel> logLevels, TestSpecs testSpecs) {
        for (LogLevel logLevel : logLevels) {
            testSpecs.add(logLevel,
                    "simple unformatted message",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S)", logger, logLevel.sourceLogLevel(), "test message"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "collapse conditional empty",
                    (builder, logger) -> {
                        builder.beginControlFlow("if( $N.isLoggable($T.$L) )",
                                logger, Level.class, logLevel.sourceLogLevelUpperCase());
                        builder.endControlFlow();
                    },
                    (builder, logger) -> {
                    }
            );

            testSpecs.add(logLevel,
                    "collapse conditional one statement",
                    (builder, logger) -> builder
                            .beginControlFlow("if( $N.isLoggable($T.$L) )",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase())
                            .addStatement("$N.$L($S)",
                                    logger, logLevel.sourceLogLevel(), "test message")
                            .endControlFlow(),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "collapse conditional two statements",
                    (builder, logger) -> builder
                            .beginControlFlow("if( $N.isLoggable($T.$L) )",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase())
                            .addStatement("$N.$L($S)",
                                    logger, logLevel.sourceLogLevel(), "test message")
                            .addStatement("$N.$L($S)",
                                    logger, logLevel.sourceLogLevel(), "test message")
                            .endControlFlow(),
                    (builder, logger) -> builder
                            .addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
                            .addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
            );

            testSpecs.add(logLevel,
                    "migrate compound conditional",
                    (builder, logger) -> builder
                            .beginControlFlow("if( $N.isLoggable($T.$L) )",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase())
                            .addStatement("$N.$L($S)",
                                    logger, logLevel.sourceLogLevel(), "test message")
                            .addStatement("dummyMethod()")
                            .endControlFlow(),
                    (builder, logger) -> builder
                            .beginControlFlow("if( $N.$L().isEnabled() )", logger, logLevel.targetLogLevel())
                            .addStatement("$N.$L().log($S)", logger, logLevel.targetLogLevel(), "test message")
                            .addStatement("dummyMethod()")
                            .endControlFlow()
            );

            testSpecs.add(logLevel,
                    "string concatenation",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($S + 1 + $S)", logger, logLevel.sourceLogLevel(), "A", "B"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S,1)", logger, logLevel.targetLogLevel(), "A%sB")
            );

            testSpecs.add(logLevel,
                    "supplier as message",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(() -> stringVar)", logger, logLevel.sourceLogLevel()),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, lazy(() -> stringVar))",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "throwable",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$L, $S, throwableVar)",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "Exception !!!"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S)",
                                    logger, logLevel.targetLogLevel(), "Exception !!!")
            );

            testSpecs.add(logLevel,
                    "unpack String.format",
                    (builder, logger) ->
                            builder.addStatement("$N.$L(String.format($S,objectVar))",
                                    logger, logLevel.sourceLogLevel(), "%s"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            if( logLevel.lazyArgs()) {
                testSpecs.add(logLevel,
                        "expensive formatting argument",
                        (builder, logger) ->
                                builder.addStatement("$N.$L(String.format($S,dummyMethod()))",
                                        logger, logLevel.sourceLogLevel(), "%s"),
                        (builder, logger) ->
                                builder.addStatement("$N.$L().log($S, lazy(() -> dummyMethod()))",
                                        logger, logLevel.targetLogLevel(), "%s")
                );
            } else {
                testSpecs.add(logLevel,
                        "expensive formatting argument",
                        (builder, logger) ->
                                builder.addStatement("$N.$L(String.format($S,dummyMethod()))",
                                        logger, logLevel.sourceLogLevel(), "%s"),
                        (builder, logger) ->
                                builder.addStatement("$N.$L().log($S, dummyMethod())",
                                        logger, logLevel.targetLogLevel(), "%s")
                );
            }

            testSpecs.add(logLevel,
                    "unpack String.format w/ Throwable",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, String.format($S,objectVar), throwableVar)",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "%s"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "log w/ custom level",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, objectVar)",
                                    logger, CustomJULLevel.class, "LEVEL_1", "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.at($T.$L).log($S, objectVar)",
                                    logger, CustomJULLevel.class, "LEVEL_1", "%s")
            );

            testSpecs.add(logLevel,
                    "log w/ parameters",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, objectVar)",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "log w/ parameters escaped",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, objectVar)",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{0} 5%"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s 5%%")
            );

            testSpecs.add(logLevel,
                    "log w/ parameters mismatched",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, new Object[] { objectVar, arrayVar, objectVar, arrayVar })",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{9} {0} {1}"),
                    (builder, logger) -> builder
                            .addComment("TODO [LoggerApiRefactoring] Invalid parameter index: 9: \"{9} {0} {1}\"")
                            .addComment("TODO [LoggerApiRefactoring] Unused parameter: objectVar")
                            .addComment("TODO [LoggerApiRefactoring] Unused parameter: arrayVar")
                            .addStatement("$N.$L().log($S, objectVar, arrayVar, objectVar, arrayVar)",
                                    logger, logLevel.targetLogLevel(), "%s %s %s")
            );

            testSpecs.add(logLevel,
                    "log w/ missing parameter",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S)",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S)",
                                    logger, logLevel.targetLogLevel(), "{0}")
            );

            testSpecs.add(logLevel,
                    "log w/ repeat parameter",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, objectVar )",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{0} {0} {0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar, objectVar, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s %s %s")
            );

            testSpecs.add(logLevel,
                    "log w/ parameters unpack Arrays.toString",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, $T.toString(arrayVar))",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{0}", Arrays.class),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, arrayVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "log w/ parameters unpack toString",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, objectVar.toString().toString())",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "log w/ parameters unpack varargs",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $S, new Object[] { objectVar })",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack Message.format",
                    (builder, logger) ->
                            builder.addStatement("$N.$L($T.format($S,objectVar))",
                                    logger, logLevel.sourceLogLevel(), MessageFormat.class, "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );

            testSpecs.add(logLevel,
                    "unpack Message.format w/ Throwable",
                    (builder, logger) ->
                            builder.addStatement("$N.log($T.$N, $T.format($S,objectVar), throwableVar)",
                                    logger, Level.class, logLevel.sourceLogLevelUpperCase(), MessageFormat.class, "{0}"),
                    (builder, logger) ->
                            builder.addStatement("$N.$L().withCause(throwableVar).log($S, objectVar)",
                                    logger, logLevel.targetLogLevel(), "%s")
            );
        }
    }
}