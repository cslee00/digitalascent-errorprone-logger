package com.digitalascent.errorprone.flogger.migrate.model;

import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;

import static java.util.Objects.requireNonNull;

public final class MethodInvocation {
    private final MethodInvocationTree tree;
    private final String methodName;

    public static MethodInvocation from( MethodInvocationTree tree ) {
        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(tree);
        if( sym == null ) {
            throw new IllegalArgumentException("Invalid tree: " + tree );
        }
        String methodName = sym.getSimpleName().toString();
        return new MethodInvocation( tree, methodName );
    }

    private MethodInvocation(MethodInvocationTree tree, String methodName) {
        this.tree = requireNonNull(tree, "tree");
        this.methodName = requireNonNull(methodName, "methodName");
    }

    public MethodInvocationTree tree() {
        return tree;
    }

    public String methodName() {
        return methodName;
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
