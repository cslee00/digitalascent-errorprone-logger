package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatConversionResult;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class Slf4jMessageFormatSpecification implements MessageFormatSpecification {
    @Override
    public MessageFormatConversionResult convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return new MessageFormatConversionResult( Slf4jMessageFormatConverter.convertMessageFormat(sourceMessageFormat), formatArguments );
    }

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return false;
    }
}
