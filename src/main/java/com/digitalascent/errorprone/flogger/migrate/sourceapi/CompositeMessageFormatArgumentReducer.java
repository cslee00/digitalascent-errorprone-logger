package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

final class CompositeMessageFormatArgumentReducer implements MessageFormatArgumentReducer{
    private final List<MessageFormatArgumentReducer> reducers;

    CompositeMessageFormatArgumentReducer() {
        ImmutableList.Builder<MessageFormatArgumentReducer> builder = ImmutableList.builder();
        builder.add(new ToStringMessageFormatArgumentReducer());
        builder.add(new ArraysToStringMessageFormatArgumentReducer());
        this.reducers = builder.build();
    }

    @Override
    public ExpressionTree reduce(ExpressionTree argument, VisitorState visitorState) {
        ExpressionTree lastReducedArgument = argument;
        for (MessageFormatArgumentReducer reducer : reducers) {
            ExpressionTree reducedArgument = reducer.reduce(lastReducedArgument,visitorState);
            if( reducedArgument != null ) {
                lastReducedArgument = reducedArgument;
            }
        }
        return lastReducedArgument == argument ? argument : reduce( lastReducedArgument, visitorState );
    }
}
