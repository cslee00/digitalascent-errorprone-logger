package com.digitalascent.errorprone.flogger;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.LazyArgs;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.slf4j.LoggerFactory;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.logging.Logger;

import static java.util.Objects.requireNonNull;

public final class TestFixtures {

    private static final FieldSpec JUL_LOGGER = FieldSpec.builder(Logger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLogger(getClass().getName())", Logger.class)
            .build();

    private static final FieldSpec COMMONS_LOGGING_LOGGER = FieldSpec.builder(Log.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLog(getClass())", LogFactory.class)
            .build();

    private static final FieldSpec SLF4J_LOGGER = FieldSpec.builder(org.slf4j.Logger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLogger(getClass())", LoggerFactory.class)
            .build();

    private static final FieldSpec FLOGGER_LOGGER = FieldSpec.builder(FluentLogger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$T.forEnclosingClass()", FluentLogger.class)
            .build();

    public static TestSourceBuilder builderWithJULLogger() {
        return new TestSourceBuilder(JUL_LOGGER);
    }


    public static TestSourceBuilder builderWithCommonsLoggingLogger() {
        return new TestSourceBuilder(COMMONS_LOGGING_LOGGER);
    }

    public static TestSourceBuilder builderWithSLF4JLogger() {
        return new TestSourceBuilder(SLF4J_LOGGER);
    }

    public static TestSourceBuilder builderWithFloggerLogger() {
        return new TestSourceBuilder(FLOGGER_LOGGER);
    }

    public static final class TestSourceBuilder {

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
                    .returns(void.class);


            MethodSpec dummyMethod = MethodSpec.methodBuilder("dummyMethod")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addStatement("$T.format($S,$S)", MessageFormat.class, "{0}", "abc")
                    .addStatement("$T.toString(new Object[0])", Arrays.class)
                    .addStatement("Object obj = $T.INSTANCE", DummySlf4JMarker.class)
                    .build();

            typeSpecBuilder.addMethod(dummyMethod);
        }

        public TestSourceBuilder code(MethodBodyCallback callback) {
            callback.callback(methodSpecBuilder, logger);
            return this;
        }

        public String build() {

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
