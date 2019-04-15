package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;

import static com.google.errorprone.matchers.Matchers.isSameType;

/**
 */
public final class Log4j2MessageFormatArgumentConverter implements MessageFormatArgumentConverter {

    private static final ImmutableList<String> LAZY_ARG_IMPORT = ImmutableList.of("com.google.common.flogger.LazyArgs.lazy");

    @Override
    public MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        if (isMessageType(argument, visitorState)) {
            String rawSource = visitorState.getSourceForNode(argument);
            if (rawSource == null) {
                return null;
            }
            String source = "lazy(() -> " + rawSource + ".getFormattedMessage())";
            return MessageFormatArgument.fromCode(source, ImmutableList.of(), LAZY_ARG_IMPORT);
        }
        return null;
    }

    private boolean isMessageType(ExpressionTree argument, VisitorState state) {
        return isSameType("org.apache.logging.log4j.message.Message").matches(argument, state);
    }
}
