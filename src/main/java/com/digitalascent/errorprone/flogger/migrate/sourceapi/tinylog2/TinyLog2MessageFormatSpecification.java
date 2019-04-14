package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatSpecification;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public final class TinyLog2MessageFormatSpecification implements MessageFormatSpecification {
    private static final Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("org.tinylog.Supplier")
    );

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return INVALID_MSG_FORMAT_TYPES.matches( messageFormatArgument, state);
    }

    @Override
    public LogMessage convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {
        return LogMessage.fromStringFormat( TinyLog2MessageFormatter.format(sourceMessageFormat), formatArguments);
    }
}
