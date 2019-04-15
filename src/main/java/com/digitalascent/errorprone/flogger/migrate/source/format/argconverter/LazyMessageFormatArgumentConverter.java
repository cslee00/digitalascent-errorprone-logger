package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;

import static com.google.errorprone.matchers.Matchers.anyMethod;

/**
 * Wrap method-invoking message format arguments in lazy( extract ) to defer evaluation until it's been determined
 * that the log level is enabled
 */
public final class LazyMessageFormatArgumentConverter extends AbstractLazyArgConverter {

    private final int lazyThresholdOrdinal;

    public LazyMessageFormatArgumentConverter(int lazyThresholdOrdinal) {
        this.lazyThresholdOrdinal = lazyThresholdOrdinal;
    }

    @Override
    protected String decorate(String rawSource) {
        return "() -> " + rawSource ;
    }

    @Override
    protected boolean matches(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        return isLazyLogLevel(targetLogLevel) && isLazyArgument(argument, visitorState);
    }

    private boolean isLazyLogLevel(TargetLogLevel targetLogLevel) {
        return targetLogLevel.ordinal() <= lazyThresholdOrdinal;
    }

    private boolean isLazyArgument(ExpressionTree argument, VisitorState visitorState) {
        return anyMethod().matches(argument, visitorState) ||
                argument instanceof NewClassTree ||
                argument instanceof NewArrayTree;
    }
}
