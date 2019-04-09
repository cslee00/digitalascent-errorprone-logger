package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.format.converter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.MessageFormatArgumentReducer;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class TinyLog2LogMessageHandler extends AbstractLogMessageHandler {
    private static final Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("org.tinylog.Supplier")
    );

    public TinyLog2LogMessageHandler(MessageFormatArgumentConverter messageFormatArgumentConverter, MessageFormatArgumentReducer messageFormatArgumentReducer) {
        super(messageFormatArgumentConverter, messageFormatArgumentReducer);
    }

    @Override
    protected boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return INVALID_MSG_FORMAT_TYPES.matches( messageFormatArgument, state);
    }

    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {
        return LogMessageModel.fromStringFormat( TinyLog2MessageFormatter.format(sourceMessageFormat), formatArguments);
    }
}
