package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

final class TinyLogMatchers {
    private static final String LOG_CLASS = "org.pmw.tinylog.Logger";

    private static final Matcher<ExpressionTree> LOGGING_METHODS = staticMethod()
            .onClass(LOG_CLASS)
            .namedAnyOf("trace","debug","info","warn","error");

    private static final Matcher<ExpressionTree> THROWABLE_MATCHER = isSubtypeOf(Throwable.class);
    private static final Matcher<Tree> LOG_TYPE_MATCHER = isSubtypeOf(LOG_CLASS);
    private static final Matcher<Tree> IMPORT_TYPES = anyOf(LOG_TYPE_MATCHER );

    static Matcher<ExpressionTree> loggingMethod() {
        return LOGGING_METHODS;
    }

    static Matcher<ExpressionTree> throwableType() {
        return THROWABLE_MATCHER;
    }

    static Matcher<Tree> logType() {
        return LOG_TYPE_MATCHER;
    }

    static Matcher<Tree> loggerImports() {
        return IMPORT_TYPES;
    }

    private TinyLogMatchers() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
