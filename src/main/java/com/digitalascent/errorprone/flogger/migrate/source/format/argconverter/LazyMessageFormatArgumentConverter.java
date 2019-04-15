package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.common.collect.ImmutableList;
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
    public MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        if (isLazyLogLevel(targetLogLevel) && isLazyArgument(argument, visitorState)) {
            String rawSource = visitorState.getSourceForNode(argument);
            if (rawSource == null) {
                return null;
            }
            return lazyArgument("() -> " + rawSource );
        }
        return null;
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
