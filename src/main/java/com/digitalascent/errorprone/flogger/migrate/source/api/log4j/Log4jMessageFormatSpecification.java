package com.digitalascent.errorprone.flogger.migrate.source.api.log4j;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatConversionResult;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class Log4jMessageFormatSpecification implements MessageFormatSpecification {

    @Override
    public MessageFormatConversionResult convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return new MessageFormatConversionResult(sourceMessageFormat, formatArguments);
    }

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return false;
    }
}
