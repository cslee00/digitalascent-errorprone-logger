package com.digitalascent.errorprone.flogger.migrate.format;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Represents a format extract to a logging message.
 */
public final class MessageFormatArgument {
    private final List<String> staticImports;
    private final List<String> imports;
    @Nullable
    private final String code;

    @Nullable
    private final ExpressionTree argument;

    private MessageFormatArgument(@Nullable String code, @Nullable ExpressionTree argument, List<String> imports,
                                  List<String> staticImports) {
        this.code = code;
        this.argument = argument;
        this.imports = ImmutableList.copyOf(imports);
        this.staticImports = ImmutableList.copyOf(staticImports);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("staticImports", staticImports)
                .add("imports", imports)
                .add("code", code)
                .add("argument", argument)
                .toString();
    }

    public static MessageFormatArgument fromExpressionTree(ExpressionTree argument) {
        return new MessageFormatArgument(null, argument, ImmutableList.of(), ImmutableList.of());
    }

    public static MessageFormatArgument fromCode(String code, List<String> imports, List<String> staticImports) {
        return new MessageFormatArgument(code, null, imports, staticImports);
    }

    public String code(VisitorState visitorState) {
        return this.code != null ? this.code : visitorState.getSourceForNode(argument);
    }

    public List<String> staticImports() {
        return staticImports;
    }

    public List<String> imports() {
        return imports;
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
