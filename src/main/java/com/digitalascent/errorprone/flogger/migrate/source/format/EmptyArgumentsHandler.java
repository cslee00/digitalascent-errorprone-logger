package com.digitalascent.errorprone.flogger.migrate.source.format;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;

public interface EmptyArgumentsHandler {
    @Nullable
    MessageFormatConversionResult handle(ExpressionTree messageFormatArgument, VisitorState state, TargetLogLevel targetLogLevel);
}
