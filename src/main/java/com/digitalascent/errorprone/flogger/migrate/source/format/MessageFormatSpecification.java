package com.digitalascent.errorprone.flogger.migrate.source.format;

import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

public interface MessageFormatSpecification {
    MessageFormatConversionResult convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<? extends ExpressionTree> processArguments, MigrationContext migrationContext);

    boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state);
}
