package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;

final class Slf4jMatchers {
    private static final String SLF4J_LOGGER_CLASS = "org.slf4j.Logger";
    private static final String SLF4J_MARKER_CLASS = "org.slf4j.Marker";
    private static String SLF4J_LOGGER_FACTORY_CLASS = "org.slf4j.LoggerFactory";

    private static final Matcher<ExpressionTree> MARKER_MATCHER = isSubtypeOf(SLF4J_MARKER_CLASS);
    private static final Matcher<ExpressionTree> CLASS_MATCHER = isSubtypeOf(Class.class);
    private static final Matcher<ExpressionTree> LOGGER_FACTORY_MATCHER = methodInvocation(staticMethod()
            .onClass(SLF4J_LOGGER_FACTORY_CLASS)
            .named("getLogger"));
    private static final Matcher<Tree> LOGGER_TYPE_MATCHER = isSubtypeOf(SLF4J_LOGGER_CLASS);
    private static final Matcher<Tree> LOGGER_FACTORY_TYPE_MATCHER = isSubtypeOf(SLF4J_LOGGER_FACTORY_CLASS);
    private static final Matcher<ExpressionTree> LOGGING_METHODS = instanceMethod()
            .onDescendantOf(SLF4J_LOGGER_CLASS)
            .namedAnyOf("trace","debug","info","warn","error");
    private static final Matcher<ExpressionTree> IS_ENABLED_METHODS = instanceMethod()
            .onDescendantOf(SLF4J_LOGGER_CLASS)
            .namedAnyOf("isTraceEnabled","isDebugEnabled","isInfoEnabled","isWarnEnabled","isErrorEnabled");
    private static final Matcher<Tree> SLF4J_LOGGER_TYPES = anyOf( LOGGER_TYPE_MATCHER, LOGGER_FACTORY_TYPE_MATCHER );

    static Matcher<ExpressionTree> loggingMethod() {
        return LOGGING_METHODS;
    }

    static Matcher<ExpressionTree> loggingEnabledMethod() {
        return IS_ENABLED_METHODS;
    }

    static Matcher<ExpressionTree> markerType() {
        return MARKER_MATCHER;
    }

    static Matcher<ExpressionTree> classType() {
        return CLASS_MATCHER;
    }

    static Matcher<ExpressionTree> loggerFactoryMethod() {
        return LOGGER_FACTORY_MATCHER;
    }

    static Matcher<Tree> loggerType() {
        return LOGGER_TYPE_MATCHER;
    }

    static Matcher<Tree> loggerImports() {
        return SLF4J_LOGGER_TYPES;
    }

    private Slf4jMatchers() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
