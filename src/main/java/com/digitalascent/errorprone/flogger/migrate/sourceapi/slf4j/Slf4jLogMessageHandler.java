package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

final class Slf4jLogMessageHandler extends AbstractLogMessageHandler {
    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return LogMessageModel.fromStringFormat( Slf4jMessageFormatConverter.convertMessageFormat(sourceMessageFormat), formatArguments );
    }
}
