package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public interface MessageFormatSpecification {
    LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> processArguments, MigrationContext migrationContext);

    boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state);
}
