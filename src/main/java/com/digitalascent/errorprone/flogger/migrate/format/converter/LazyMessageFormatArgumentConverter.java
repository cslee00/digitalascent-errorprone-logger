package com.digitalascent.errorprone.flogger.migrate.format.converter;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;

import static com.google.errorprone.matchers.Matchers.anyMethod;

public final class LazyMessageFormatArgumentConverter implements MessageFormatArgumentConverter {

    private static final ImmutableList<String> LAZY_ARG_IMPORT = ImmutableList.of("com.google.common.flogger.LazyArgs.lazy");

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
            String source = "lazy(" + rawSource + ")";
            return MessageFormatArgument.fromCode(source, ImmutableList.of(), LAZY_ARG_IMPORT);
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
