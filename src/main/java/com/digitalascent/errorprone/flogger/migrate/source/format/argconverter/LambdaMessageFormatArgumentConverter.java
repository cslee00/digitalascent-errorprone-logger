package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;

/**
 */
public final class LambdaMessageFormatArgumentConverter extends AbstractLazyArgConverter {

    @Override
    public MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        if (isLambdaMessageFormat(argument)) {
            String rawSource = visitorState.getSourceForNode(argument);
            if (rawSource == null) {
                return null;
            }
            return lazyArgument( rawSource );
        }
        return null;
    }

    private boolean isLambdaMessageFormat(ExpressionTree argument) {
        return argument instanceof LambdaExpressionTree;
    }
}
