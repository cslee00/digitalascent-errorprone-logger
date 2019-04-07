package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

import com.digitalascent.errorprone.flogger.migrate.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.LogMessageModel;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

final class CommonsLoggingLogMessageHandler extends AbstractLogMessageHandler {
    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {
        // no conversion required as Commons Logging doesn't support message formats
        return LogMessageModel.fromStringFormat(sourceMessageFormat, formatArguments);
    }
}
