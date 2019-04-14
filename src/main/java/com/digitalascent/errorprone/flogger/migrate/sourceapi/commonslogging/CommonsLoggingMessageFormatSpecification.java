package com.digitalascent.errorprone.flogger.migrate.sourceapi.commonslogging;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatConversionResult;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class CommonsLoggingMessageFormatSpecification implements MessageFormatSpecification {

    @Override
    public MessageFormatConversionResult convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<? extends ExpressionTree> formatArguments,
                                                              MigrationContext migrationContext) {
        // no conversion required as Commons Logging doesn't support message formats
        return new MessageFormatConversionResult(sourceMessageFormat, formatArguments);
    }

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return false;
    }
}
