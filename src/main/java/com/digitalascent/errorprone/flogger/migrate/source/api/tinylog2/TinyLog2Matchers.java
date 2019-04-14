package com.digitalascent.errorprone.flogger.migrate.source.api.tinylog2;

import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.Tree;

import static com.google.errorprone.matchers.Matchers.anyOf;
import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.staticMethod;

final class TinyLog2Matchers {
    private static final String LOG_CLASS = "org.tinylog.Logger";

    private static final Matcher<ExpressionTree> LOGGING_METHODS = anyOf(staticMethod()
            .onClass(LOG_CLASS)
            .namedAnyOf("trace","debug","info","warn","error"),
            instanceMethod().onDescendantOf("org.tinylog.TaggedLogger")
            .namedAnyOf("trace","debug","info","warn","error"));

    private static final Matcher<ExpressionTree> THROWABLE_MATCHER = isSubtypeOf(Throwable.class);
    private static final Matcher<Tree> LOG_TYPE_MATCHER = isSubtypeOf(LOG_CLASS);
    private static final Matcher<Tree> IMPORT_TYPES = anyOf(LOG_TYPE_MATCHER, isSubtypeOf("org.tinylog.TaggedLogger") );

    static Matcher<ExpressionTree> loggingMethod() {
        return LOGGING_METHODS;
    }

    static Matcher<ExpressionTree> throwableType() {
        return THROWABLE_MATCHER;
    }

    static Matcher<Tree> loggerImports() {
        return IMPORT_TYPES;
    }

    private TinyLog2Matchers() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
