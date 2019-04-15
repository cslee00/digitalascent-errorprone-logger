package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

abstract class AbstractLazyArgConverter implements MessageFormatArgumentConverter{
    private static final ImmutableList<String> LAZY_ARG_IMPORT = ImmutableList.of("com.google.common.flogger.LazyArgs.lazy");

    @Override
    public final MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        if (matches(argument, visitorState, targetLogLevel)) {
            String rawSource = visitorState.getSourceForNode(argument);
            if (rawSource == null) {
                return null;
            }
            String source = decorate( rawSource );
            return lazyArgument( source );
        }
        return null;
    }

    protected abstract String decorate(String rawSource);

    protected abstract boolean matches(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel);

    private MessageFormatArgument lazyArgument(String code ) {
        String source = "lazy(" + code + ")";
        return MessageFormatArgument.fromCode(source, ImmutableList.of(), LAZY_ARG_IMPORT);
    }
}
