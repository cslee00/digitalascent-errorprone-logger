package com.digitalascent.errorprone.flogger.migrate.source.api.tinylog2;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatConversionResult;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class TinyLog2MessageFormatSpecification implements MessageFormatSpecification {
    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return false;
    }

    @Override
    public MessageFormatConversionResult convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return new MessageFormatConversionResult(TinyLog2MessageFormatter.format(sourceMessageFormat), formatArguments);
    }
}
