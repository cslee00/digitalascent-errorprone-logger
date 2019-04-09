package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.format.converter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.MessageFormatArgumentReducer;

import java.util.List;

public final class Slf4jLogMessageHandler extends AbstractLogMessageHandler {
    public Slf4jLogMessageHandler(MessageFormatArgumentConverter messageFormatArgumentConverter, MessageFormatArgumentReducer messageFormatArgumentReducer) {
        super(messageFormatArgumentConverter, messageFormatArgumentReducer);
    }

    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {
        return LogMessageModel.fromStringFormat( Slf4jMessageFormatConverter.convertMessageFormat(sourceMessageFormat), formatArguments );
    }
}
