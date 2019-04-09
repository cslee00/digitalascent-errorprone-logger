package com.digitalascent.errorprone.flogger.migrate.format.converter;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

public interface MessageFormatArgumentConverter {
    MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel);
}
