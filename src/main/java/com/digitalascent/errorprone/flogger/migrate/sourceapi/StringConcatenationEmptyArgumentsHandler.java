package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.BinaryTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.errorprone.matchers.Matchers.allOf;
import static com.google.errorprone.matchers.Matchers.anything;
import static com.google.errorprone.matchers.Matchers.binaryTree;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.kindIs;
import static com.sun.source.tree.Tree.Kind.PLUS;

/**
 * Converts string concatenation into a parameterized format/argument construct
 */
final class StringConcatenationEmptyArgumentsHandler implements EmptyArgumentsHandler {
    private static final Matcher<BinaryTree> STRING_CONCATENATION_MATCHER =
            allOf(kindIs(PLUS),
                    binaryTree(anything(), isSameType("java.lang.String")));

    @Nullable
    @Override
    public MessageFormatConversionResult handle(ExpressionTree messageFormatArgument, VisitorState state, TargetLogLevel targetLogLevel) {
        if (!(messageFormatArgument instanceof BinaryTree)) {
            return null;
        }

        BinaryTree binaryTree = (BinaryTree) messageFormatArgument;
        if (!STRING_CONCATENATION_MATCHER.matches(binaryTree, state)) {
            return null;
        }

        StringBuilder sb = new StringBuilder();
        List<ExpressionTree> arguments = new ArrayList<>();

        handleBinaryTree(binaryTree, sb, arguments);

        return new MessageFormatConversionResult(sb.toString(), arguments);
    }

    private void handleBinaryTree(BinaryTree binaryTree, StringBuilder sb, List<ExpressionTree> arguments) {
        ExpressionTree lhs = binaryTree.getLeftOperand();
        ExpressionTree rhs = binaryTree.getRightOperand();

        if (lhs instanceof BinaryTree) {
            handleBinaryTree((BinaryTree) lhs, sb, arguments);
        } else {
            if (lhs instanceof LiteralTree) {
                handleLiteral(sb, arguments, lhs);
            } else {
                addArgument(sb, arguments, lhs);
            }
        }

        if (rhs instanceof LiteralTree) {
            handleLiteral(sb, arguments, rhs);
        } else {
            addArgument(sb, arguments, rhs);
        }
    }

    private void handleLiteral(StringBuilder sb, List<ExpressionTree> arguments, ExpressionTree node) {
        LiteralTree literalTree = (LiteralTree) node;
        Object value = literalTree.getValue();
        if (value instanceof String) {
            sb.append(value);
        } else {
            addArgument(sb, arguments, node);
        }
    }

    private void addArgument(StringBuilder sb, List<ExpressionTree> arguments, ExpressionTree node) {
        sb.append("%s");
        arguments.add(node);
    }
}
