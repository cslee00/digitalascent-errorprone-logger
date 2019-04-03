package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;

final class JULMatchers {
    private static final String LOGGER_CLASS = "java.util.logging.Logger";
    private static String LOGGER_FACTORY_CLASS = "java.util.logging.Logger";

    private static final Matcher<ExpressionTree> STRING_MATCHER = isSubtypeOf(String.class);
    private static final Matcher<ExpressionTree> LOGGER_FACTORY_MATCHER = methodInvocation(staticMethod()
            .onClass(LOGGER_FACTORY_CLASS)
            .named("getLogger"));
    private static final Matcher<Tree> LOGGER_TYPE_MATCHER = isSubtypeOf(LOGGER_CLASS);
    private static final Matcher<Tree> LOGGER_FACTORY_TYPE_MATCHER = isSubtypeOf(LOGGER_FACTORY_CLASS);
    private static final Matcher<ExpressionTree> LOGGING_METHODS = instanceMethod()
            .onDescendantOf(LOGGER_CLASS)
            .namedAnyOf("finest", "finer", "fine", "config", "info", "warning", "severe", "log" );
    private static final Matcher<ExpressionTree> IS_ENABLED_METHODS = instanceMethod()
            .onDescendantOf(LOGGER_CLASS)
            .namedAnyOf("isLoggable");
    private static final Matcher<Tree> LOG_LEVEL_TYPE = isSameType("java.util.logging.Level");
    private static final Matcher<Tree> IMPORT_TYPES = anyOf( LOGGER_TYPE_MATCHER, LOGGER_FACTORY_TYPE_MATCHER, LOG_LEVEL_TYPE );

    static Matcher<Tree> logLevelType() { return LOG_LEVEL_TYPE; }
    static Matcher<ExpressionTree> loggingMethod() {
        return LOGGING_METHODS;
    }

    static Matcher<ExpressionTree> loggingEnabledMethod() {
        return IS_ENABLED_METHODS;
    }

    static Matcher<ExpressionTree> stringType() {
        return STRING_MATCHER;
    }

    static Matcher<ExpressionTree> loggerFactoryMethod() {
        return LOGGER_FACTORY_MATCHER;
    }

    static Matcher<Tree> loggerType() {
        return LOGGER_TYPE_MATCHER;
    }

    static Matcher<Tree> loggerImports() {
        return IMPORT_TYPES;
    }

    private JULMatchers() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
