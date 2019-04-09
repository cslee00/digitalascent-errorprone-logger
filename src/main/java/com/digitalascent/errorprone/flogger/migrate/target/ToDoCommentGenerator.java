package com.digitalascent.errorprone.flogger.migrate.target;

import com.google.common.base.CharMatcher;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;

final class ToDoCommentGenerator {
    private static final CharMatcher NEWLINE = CharMatcher.anyOf("\r\n");
    public static String singleLineCommentForNode(String text, Tree node, VisitorState visitorState) {
        return "// TODO [LoggerApiRefactoring] " + NEWLINE.replaceFrom( text, "\\n" ) + "\n" + ASTUtil.determineIndent(node,visitorState);
    }

    private ToDoCommentGenerator() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
