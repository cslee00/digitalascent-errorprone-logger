package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog;

import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.List;

final class TinyLogLogMessageHandler extends AbstractLogMessageHandler {
    private static final Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("org.pmw.tinylog.Supplier")
    );

    @Nullable
    @Override
    protected LogMessageModel customProcessing(ExpressionTree messageFormatArgument, VisitorState state, @Nullable ExpressionTree thrownArgument) {
        if( INVALID_MSG_FORMAT_TYPES.matches( messageFormatArgument, state)) {
            throw new SkipLogMethodException("Unable to convert message format: " + messageFormatArgument);
        }
        return null;
    }

    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return LogMessageModel.fromStringFormat( TinyLogMessageFormatter.format(sourceMessageFormat), formatArguments);
    }
}
