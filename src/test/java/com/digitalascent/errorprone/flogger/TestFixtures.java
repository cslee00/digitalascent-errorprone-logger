package com.digitalascent.errorprone.flogger;

import com.google.common.flogger.FluentLogger;
import com.google.common.flogger.LazyArgs;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeSpec;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.io.StringWriter;
import java.text.MessageFormat;

import static java.util.Objects.requireNonNull;

public final class TestFixtures {

    private static final FieldSpec COMMONS_LOGGING_LOGGER = FieldSpec.builder(Log.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.FINAL)
            .initializer("$T.getLog(getClass())", LogFactory.class)
            .build();

    private static final FieldSpec FLOGGER_LOGGER = FieldSpec.builder(FluentLogger.class, "logger")
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer("$T.forEnclosingClass()", FluentLogger.class)
            .build();

    public static TestSourceBuilder builderWithCommonsLoggingLogger() {
        return new TestSourceBuilder(COMMONS_LOGGING_LOGGER);
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
                    .addStatement("Throwable throwableVar = new Throwable()")
                    .returns(void.class);


            MethodSpec dummyMethod = MethodSpec.methodBuilder("dummyMethod")
                    .addModifiers(Modifier.PUBLIC)
                    .returns(void.class)
                    .addStatement("$T.format($S,$S)", MessageFormat.class, "{0}", "abc")
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
