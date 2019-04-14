package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class Log4jMessageFormatSpecification implements MessageFormatSpecification {

    @Override
    public LogMessage convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {
        return LogMessage.fromStringFormat(sourceMessageFormat, formatArguments);
    }

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return false;
    }
}
