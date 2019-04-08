package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;

final class Log4j2Matchers {
    private static final String LOG4J2_LOGGER_CLASS = "org.apache.logging.log4j.Logger";
    private static final String LOG4J2_MARKER_CLASS = "org.apache.logging.log4j.Marker";
    private static String LOG4J2_LOG_MANAGER_CLASS = "org.apache.logging.log4j.LogManager";

    private static final Matcher<ExpressionTree> IS_ENABLED_METHODS = instanceMethod()
            .onDescendantOf(LOG4J2_LOGGER_CLASS)
            .namedAnyOf("isTraceEnabled","isDebugEnabled","isInfoEnabled","isWarnEnabled","isErrorEnabled", "isFatalEnabled", "isEnabled");

    private static final Matcher<ExpressionTree> LOGGING_METHODS = instanceMethod()
            .onDescendantOf(LOG4J2_LOGGER_CLASS)
            .namedAnyOf("trace","debug","info","warn","error","fatal","log");

    private static final Matcher<ExpressionTree> LOG_MANAGER_MATCHER = methodInvocation(staticMethod()
            .onClass(LOG4J2_LOG_MANAGER_CLASS)
            .namedAnyOf("getLogger", "getFormatterLogger"));

    private static final Matcher<ExpressionTree> MARKER_MATCHER = isSubtypeOf(LOG4J2_MARKER_CLASS);
    private static final Matcher<Tree> LOGGER_TYPE_MATCHER = isSubtypeOf(LOG4J2_LOGGER_CLASS);
    private static final Matcher<Tree> LOGGER_FACTORY_TYPE_MATCHER = isSubtypeOf(LOG4J2_LOG_MANAGER_CLASS);
    private static final Matcher<Tree> LOG4J2_LOGGER_TYPES = anyOf( LOGGER_TYPE_MATCHER, LOGGER_FACTORY_TYPE_MATCHER, isSubtypeOf("org.apache.logging.log4j.Level") );

    static Matcher<ExpressionTree> loggingMethod() {
        return LOGGING_METHODS;
    }

    static Matcher<ExpressionTree> loggingEnabledMethod() {
        return IS_ENABLED_METHODS;
    }

    static Matcher<ExpressionTree> markerType() {
        return MARKER_MATCHER;
    }

    static Matcher<ExpressionTree> logManagerMethod() {
        return LOG_MANAGER_MATCHER;
    }

    static Matcher<Tree> loggerImports() {
        return LOG4J2_LOGGER_TYPES;
    }

    private Log4j2Matchers() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
