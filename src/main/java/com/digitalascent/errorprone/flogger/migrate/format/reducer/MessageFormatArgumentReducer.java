package com.digitalascent.errorprone.flogger.migrate.format.reducer;

import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

public interface MessageFormatArgumentReducer {
    ExpressionTree reduce(ExpressionTree argument, VisitorState visitorState);
}
