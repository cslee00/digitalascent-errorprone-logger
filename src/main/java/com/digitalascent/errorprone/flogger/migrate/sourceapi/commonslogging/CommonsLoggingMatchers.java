package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;

final class CommonsLoggingMatchers {
    private static final String LOG_CLASS = "org.apache.commons.logging.Log";
    private static String LOG_FACTORY_CLASS = "org.apache.commons.logging.LogFactory";

    private static final Matcher<ExpressionTree> IS_ENABLED_METHODS = instanceMethod()
            .onDescendantOf(LOG_CLASS)
            .namedAnyOf("isTraceEnabled","isDebugEnabled","isInfoEnabled","isWarnEnabled","isErrorEnabled", "isFatalEnabled");

    private static final Matcher<ExpressionTree> LOGGING_METHODS = instanceMethod()
            .onDescendantOf(LOG_CLASS)
            .namedAnyOf("trace","debug","info","warn","error","fatal");

    private static final Matcher<ExpressionTree> LOG_FACTORY_MATCHER = methodInvocation(staticMethod()
            .onClass(LOG_FACTORY_CLASS)
            .namedAnyOf("getLog"));

    private static final Matcher<ExpressionTree> CLASS_MATCHER = isSubtypeOf(Class.class);
    private static final Matcher<Tree> LOG_TYPE_MATCHER = isSubtypeOf(LOG_CLASS);
    private static final Matcher<Tree> LOGGER_FACTORY_TYPE_MATCHER = isSubtypeOf(LOG_FACTORY_CLASS);
    private static final Matcher<Tree> IMPORT_TYPES = anyOf(LOG_TYPE_MATCHER, LOGGER_FACTORY_TYPE_MATCHER );

    static Matcher<ExpressionTree> loggingMethod() {
        return LOGGING_METHODS;
    }

    static Matcher<ExpressionTree> loggingEnabledMethod() {
        return IS_ENABLED_METHODS;
    }

    static Matcher<ExpressionTree> classType() {
        return CLASS_MATCHER;
    }

    static Matcher<ExpressionTree> logFactoryMethod() {
        return LOG_FACTORY_MATCHER;
    }

    static Matcher<Tree> logType() {
        return LOG_TYPE_MATCHER;
    }

    static Matcher<Tree> loggerImports() {
        return IMPORT_TYPES;
    }

    private CommonsLoggingMatchers() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
