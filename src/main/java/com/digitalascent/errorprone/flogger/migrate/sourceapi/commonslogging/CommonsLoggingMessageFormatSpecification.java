package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class CommonsLoggingMessageFormatSpecification implements MessageFormatSpecification {

    @Override
    public LogMessage convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<MessageFormatArgument> formatArguments,
                                           MigrationContext migrationContext) {
        // no conversion required as Commons Logging doesn't support message formats
        return LogMessage.fromStringFormat(sourceMessageFormat, formatArguments);
    }

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return false;
    }
}
