package com.digitalascent.errorprone.flogger;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.LazyArgs;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.LogManager;
import org.apache.logging.log4j.message.Message;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

final class TestFixtures {

    private static final FieldSpec JUL_LOGGER = FieldSpec.builder(Logger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLogger(getClass().getName())", Logger.class)
            .build();

    private static final FieldSpec COMMONS_LOGGING_LOGGER = FieldSpec.builder(Log.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLog(getClass())", LogFactory.class)
            .build();

    private static final FieldSpec LOG4J_LOGGER = FieldSpec.builder(org.apache.log4j.Logger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLogger(getClass())", LogManager.class)
            .build();

    private static final FieldSpec LOG4J2_LOGGER = FieldSpec.builder(org.apache.logging.log4j.Logger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLogger(getClass())", org.apache.logging.log4j.LogManager.class)
            .build();

    private static final FieldSpec SLF4J_LOGGER = FieldSpec.builder(org.slf4j.Logger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLogger(getClass())", LoggerFactory.class)
            .build();

    private static final FieldSpec FLOGGER_LOGGER = FieldSpec.builder(FluentLogger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$T.forEnclosingClass()", FluentLogger.class)
            .build();

    static TestSourceBuilder builderWithJULLogger() {
        return new TestSourceBuilder(JUL_LOGGER);
    }


    static TestSourceBuilder builderWithCommonsLoggingLogger() {
        return new TestSourceBuilder(COMMONS_LOGGING_LOGGER);
    }

    static TestSourceBuilder builderWithLog4JLogger() {
        return new TestSourceBuilder(LOG4J_LOGGER);
    }

    static TestSourceBuilder builderWithLog4J2Logger() {
        return new TestSourceBuilder(LOG4J2_LOGGER);
    }

    static TestSourceBuilder builderWithSLF4JLogger() {
        return new TestSourceBuilder(SLF4J_LOGGER);
    }

    static TestSourceBuilder builderWithFloggerLogger() {
        return new TestSourceBuilder(FLOGGER_LOGGER);
    }

    static final class TestSourceBuilder {

        private final TypeSpec.Builder typeSpecBuilder;
        private final MethodSpec.Builder methodSpecBuilder;
        private final FieldSpec logger;

        private TestSourceBuilder(FieldSpec logger) {
            this.logger = requireNonNull(logger, "logger");
            typeSpecBuilder = TypeSpec.classBuilder("TestClass")
                    .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
                    .addField(logger);

            methodSpecBuilder = MethodSpec.methodBuilder("testMethod")
                    .addModifiers(Modifier.PUBLIC)
                    .addStatement("Object objectVar = new Object()")
                    .addStatement("String stringVar = \"foo\"")
                    .addStatement("Object[] arrayVar = new Object[] { stringVar, objectVar }")
                    .addStatement("Throwable throwableVar = new Throwable()")
                    .addStatement("$T log4j2Message = new $T()", Message.class, DummyLog4j2Message.class)
                    .returns(void.class);


            MethodSpec dummyMethod = MethodSpec.methodBuilder("dummyMethod")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addStatement("$T.format($S,$S)", MessageFormat.class, "{0}", "abc")
                    .addStatement("$T.toString(new Object[0])", Arrays.class)
                    .addStatement("Object dummySlf4JMarker = $T.INSTANCE", DummySlf4JMarker.class)
                    .addStatement("Object dummyLog4j2Marker = $T.INSTANCE", DummyLog4J2Marker.class)
                    .build();

            typeSpecBuilder.addMethod(dummyMethod);
        }

        TestSourceBuilder code(MethodBodyCallback callback) {
            callback.callback(methodSpecBuilder, logger);
            return this;
        }

        String build() {

            typeSpecBuilder.addMethod(methodSpecBuilder.build());

            JavaFile javaFile = JavaFile.builder("com.digitalascent.test", typeSpecBuilder.build())
                    .addStaticImport(LazyArgs.class, "lazy")
                    .build();
            try {
                StringWriter stringWriter = new StringWriter(1024);
                javaFile.writeTo(stringWriter);
                stringWriter.close();
                return stringWriter.toString();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @FunctionalInterface
    public interface MethodBodyCallback {
        void callback(MethodSpec.Builder builder, FieldSpec logger);
    }

    private TestFixtures() {
        throw new AssertionError("Unable to instantiate " + getClass());
    }
}
