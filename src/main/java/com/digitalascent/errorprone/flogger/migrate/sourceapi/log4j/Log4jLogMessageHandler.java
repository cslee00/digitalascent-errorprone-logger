package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.format.converter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.MessageFormatArgumentReducer;

import java.util.List;

public final class Log4jLogMessageHandler extends AbstractLogMessageHandler {

    public Log4jLogMessageHandler(MessageFormatArgumentConverter messageFormatArgumentConverter,
                                  MessageFormatArgumentReducer messageFormatArgumentReducer) {
        super(messageFormatArgumentConverter, messageFormatArgumentReducer);
    }

    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {
        return LogMessageModel.fromStringFormat(sourceMessageFormat, formatArguments);
    }
}
