package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.base.Objects;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;

public final class MessageFormatArgument {
    @Nullable
    private final String code;

    @Nullable
    private final ExpressionTree argument;

    private MessageFormatArgument(@Nullable String code, @Nullable ExpressionTree argument) {
        this.code = code;
        this.argument = argument;
    }


    public static MessageFormatArgument fromExpressionTree(ExpressionTree argument) {
        return new MessageFormatArgument(null, argument);
    }

    public static MessageFormatArgument fromCode(String code) {
        return new MessageFormatArgument(code, null);
    }

    public String code(VisitorState visitorState) {
        return this.code != null ? this.code : visitorState.getSourceForNode(argument);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageFormatArgument that = (MessageFormatArgument) o;
        return Objects.equal(code, that.code) &&
                Objects.equal(argument, that.argument);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(code, argument);
    }
}
