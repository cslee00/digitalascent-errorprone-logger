package com.digitalascent.errorprone.flogger.migrate.format.converter;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class CompositeMessageFormatArgumentConverter implements MessageFormatArgumentConverter {
    private final List<MessageFormatArgumentConverter> converters;

    public CompositeMessageFormatArgumentConverter(List<MessageFormatArgumentConverter> converters) {
        this.converters = ImmutableList.copyOf(converters);
    }

    @Override
    public MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        for (MessageFormatArgumentConverter converter : converters) {
            MessageFormatArgument messageFormatArgument = converter.convert(argument,visitorState, targetLogLevel);
            if( messageFormatArgument != null ) {
                return messageFormatArgument;
            }
        }
        return MessageFormatArgument.fromExpressionTree(argument);
    }
}
