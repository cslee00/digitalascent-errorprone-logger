package com.digitalascent.errorprone.flogger.migrate.target;

import com.google.common.base.CharMatcher;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.Tree;
import com.sun.tools.javac.tree.JCTree;

final class ASTUtil {
    private static final CharMatcher PREV_LINE_MATCHER = CharMatcher.anyOf("\r\n");

    static CharSequence determineIndent(Tree tree, VisitorState state) {
        JCTree node = (JCTree) tree;
        int nodeStartPosition = node.getStartPosition();

        int startPosition = Math.max(nodeStartPosition - 100, 0);
        CharSequence charSequence = state.getSourceCode().subSequence(startPosition, nodeStartPosition).toString();
        int lastIdx = PREV_LINE_MATCHER.lastIndexIn(charSequence);
        return charSequence.subSequence(lastIdx + 1, charSequence.length());
    }

    private ASTUtil() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
