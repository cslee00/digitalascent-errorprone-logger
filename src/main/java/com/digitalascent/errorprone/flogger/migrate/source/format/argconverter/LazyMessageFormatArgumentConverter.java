package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;

import java.util.function.Predicate;

import static com.google.errorprone.matchers.Matchers.anyMethod;
import static java.util.Objects.requireNonNull;

/**
 * Wrap method-invoking message format arguments in lazy( extract ) to defer evaluation until it's been determined
 * that the log level is enabled
 */
public final class LazyMessageFormatArgumentConverter extends AbstractLazyArgConverter {

    private final Predicate<TargetLogLevel> lazyLogLevelPredicate;

    public LazyMessageFormatArgumentConverter(Predicate<TargetLogLevel> lazyLogLevelPredicate ) {
        this.lazyLogLevelPredicate = requireNonNull(lazyLogLevelPredicate, "lazyLogLevelPredicate");
    }

    @Override
    protected String decorate(String rawSource) {
        return "() -> " + rawSource;
    }

    @Override
    protected boolean matches(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        return isLazyLogLevel(targetLogLevel) && isLazyArgument(argument, visitorState);
    }

    private boolean isLazyLogLevel(TargetLogLevel targetLogLevel) {
        return lazyLogLevelPredicate.test(targetLogLevel);
    }

    // TODO - handle method invocations
    private boolean isLazyArgument(ExpressionTree argument, VisitorState visitorState) {
        return anyMethod().matches(argument, visitorState) ||
                argument instanceof NewClassTree ||
                argument instanceof NewArrayTree;
    }
}
