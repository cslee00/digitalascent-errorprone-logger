package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.base.CharMatcher;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;

public final class ToDoCommentGenerator {
    private static final CharMatcher NEWLINE = CharMatcher.anyOf("\r\n");
    public static String singleLineCommentForNode(String text, Tree tree, VisitorState visitorState) {
        return "// TODO [LoggerApiRefactoring] " + NEWLINE.replaceFrom( text, "\\n" ) + "\n" + ASTUtil.determineIndent(tree,visitorState);
    }

    private ToDoCommentGenerator() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
