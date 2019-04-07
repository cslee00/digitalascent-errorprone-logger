package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.MessageFormatArgument;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

public interface MessageFormatArgumentConverter {
    MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState );
}
