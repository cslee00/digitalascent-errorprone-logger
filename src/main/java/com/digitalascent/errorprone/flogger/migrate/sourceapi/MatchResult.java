package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.sun.source.tree.ExpressionTree;

public final class MatchResult {
    private final int index;
    private final ExpressionTree argument;

    MatchResult(int index, ExpressionTree argument) {
        this.index = index;
        this.argument = argument;
    }

    public int index() {
        return index;
    }

    public ExpressionTree argument() {
        return argument;
    }
}
