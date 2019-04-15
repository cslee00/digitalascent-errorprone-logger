package com.digitalascent.errorprone.flogger.migrate.source.format.reducer;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class CompositeMessageFormatArgumentReducer implements MessageFormatArgumentReducer {
    private final List<MessageFormatArgumentReducer> reducers;

    public CompositeMessageFormatArgumentReducer(List<MessageFormatArgumentReducer> reducers) {
        this.reducers = ImmutableList.copyOf(reducers);
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
        // recurse after reduction to enable subsequent reductions;
        // stop when there are no more reductions for the supplied argument
        return lastReducedArgument == argument ? argument : reduce( lastReducedArgument, visitorState );
    }
}
