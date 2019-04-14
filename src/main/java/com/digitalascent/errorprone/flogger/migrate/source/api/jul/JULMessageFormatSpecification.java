package com.digitalascent.errorprone.flogger.migrate.source.api.jul;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormat;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatConversionResult;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class JULMessageFormatSpecification implements MessageFormatSpecification {

    private static final com.google.errorprone.matchers.Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("java.util.function.Supplier")
    );

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
       return INVALID_MSG_FORMAT_TYPES.matches(messageFormatArgument,state);
    }

    @Override
    public MessageFormatConversionResult convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return MessageFormat.convertJavaTextMessageFormat( sourceMessageFormat,formatArguments);
    }
}
