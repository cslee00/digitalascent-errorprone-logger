package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.VariableTree;
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.code.Symbol;

import javax.lang.model.element.ElementKind;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

/**
 * Collects class member variables matching the provided predicate
 */
final class MemberVariableScanner extends TreeScanner<Void, VisitorState> {
    private final List<VariableTree> loggerVariables = new ArrayList<>();
    private final Predicate<VariableTree> loggerVariablePredicate;

    MemberVariableScanner(Predicate<VariableTree> loggerVariablePredicate) {
        this.loggerVariablePredicate = requireNonNull(loggerVariablePredicate, "loggerVariablePredicate");
    }

    @Override
    public Void visitVariable(VariableTree tree, VisitorState visitorState) {
        Symbol.VarSymbol sym = ASTHelpers.getSymbol(tree);
        if (sym == null || sym.getKind() != ElementKind.FIELD) {
            return super.visitVariable(tree, visitorState);
        }
        if (loggerVariablePredicate.test(tree)) {
            loggerVariables.add(tree);
        }
        return super.visitVariable(tree, visitorState);
    }

    List<VariableTree> loggerVariables() {
        return ImmutableList.copyOf(loggerVariables);
    }
}
