package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.format.converter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.MessageFormatArgumentReducer;

import java.util.List;

public final class CommonsLoggingLogMessageHandler extends AbstractLogMessageHandler {
    public CommonsLoggingLogMessageHandler(MessageFormatArgumentConverter messageFormatArgumentConverter,
                                    MessageFormatArgumentReducer messageFormatArgumentReducer) {
        super(messageFormatArgumentConverter, messageFormatArgumentReducer);
    }

    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {
        // no conversion required as Commons Logging doesn't support message formats
        return LogMessageModel.fromStringFormat(sourceMessageFormat, formatArguments);
    }
}
