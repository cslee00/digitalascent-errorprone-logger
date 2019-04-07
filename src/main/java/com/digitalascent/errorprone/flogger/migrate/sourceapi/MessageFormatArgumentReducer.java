package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

public interface MessageFormatArgumentReducer {
    ExpressionTree reduce(ExpressionTree argument, VisitorState visitorState);
}
