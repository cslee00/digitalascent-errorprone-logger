package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;


/**
 * Converts an ExpressionTree into a MessageFormatArgument, which may represent the same ExpressionTree (unchanged)
 * or a generated code construct
 */
public interface MessageFormatArgumentConverter {
    MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel);
}
