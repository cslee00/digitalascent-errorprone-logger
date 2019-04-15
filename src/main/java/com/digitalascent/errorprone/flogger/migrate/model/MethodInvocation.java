package com.digitalascent.errorprone.flogger.migrate.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;

import static java.util.Objects.requireNonNull;

public final class MethodInvocation {
    private final MethodInvocationTree tree;
    private final String methodName;
    private final VisitorState state;

    public static MethodInvocation from(MethodInvocationTree tree, VisitorState state) {
        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(tree);
        if( sym == null ) {
            throw new IllegalArgumentException("Invalid tree: " + tree );
        }
        String methodName = sym.getSimpleName().toString();
        return new MethodInvocation( tree, methodName, state );
    }

    private MethodInvocation(MethodInvocationTree tree, String methodName, VisitorState state) {
        this.tree = requireNonNull(tree, "tree");
        this.methodName = requireNonNull(methodName, "methodName");
        this.state = requireNonNull(state, "state");
    }

    public MethodInvocationTree tree() {
        return tree;
    }

    public String methodName() {
        return methodName;
    }

    public VisitorState state() {
        return state;
    }

    public ExpressionTree firstArgument() {
        return tree.getArguments().get(0);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MethodInvocation that = (MethodInvocation) o;
        return Objects.equal(tree, that.tree);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(tree);
    }

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("tree", tree)
                .toString();
    }
}
