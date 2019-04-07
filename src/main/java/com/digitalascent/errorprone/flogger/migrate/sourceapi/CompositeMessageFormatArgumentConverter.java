package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.MessageFormatArgument;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

final class CompositeMessageFormatArgumentConverter implements MessageFormatArgumentConverter{
    private final List<MessageFormatArgumentConverter> converters;

    CompositeMessageFormatArgumentConverter() {
        ImmutableList.Builder<MessageFormatArgumentConverter> builder = ImmutableList.builder();
//        builder.add(new ToStringMessageFormatArgumentReducer());
        this.converters = builder.build();
    }

    @Override
    public MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState) {
        for (MessageFormatArgumentConverter converter : converters) {
            MessageFormatArgument messageFormatArgument = converter.convert(argument,visitorState);
            if( messageFormatArgument != null ) {
                return messageFormatArgument;
            }
        }
        return MessageFormatArgument.fromExpressionTree(argument);
    }
}
