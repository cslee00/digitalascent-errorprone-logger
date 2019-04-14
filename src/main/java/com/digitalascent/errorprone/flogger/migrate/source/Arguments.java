package com.digitalascent.errorprone.flogger.migrate.source;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

import java.util.List;
import java.util.Optional;

import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.staticFieldAccess;

public final class Arguments {
    private static final Matcher<ExpressionTree> STRING_MATCHER = isSubtypeOf(String.class);

    static List<? extends ExpressionTree> removeLast(List<? extends ExpressionTree> expressions) {
        if (expressions.size() <= 1) {
            return ImmutableList.of();
        }
        return expressions.subList(0, expressions.size() - 1);
    }

    public static List<? extends ExpressionTree> maybeUnpackVarArgs(List<? extends ExpressionTree> arguments, VisitorState state) {
        if (arguments.size() == 1) {
            ExpressionTree argument = arguments.get(0);
            // if Object[] then unpack
            if (Matchers.isArrayType().matches(argument, state)) {
                JCTree.JCNewArray newArray = (JCTree.JCNewArray) argument;
                return newArray.elems;
            }
        }

        return arguments;
    }

    public static List<? extends ExpressionTree> prependArgument(List<? extends ExpressionTree> arguments, ExpressionTree argument) {
        ImmutableList.Builder<ExpressionTree> builder = ImmutableList.builder();
        return builder
                .add(argument)
                .addAll(arguments)
                .build();
    }

    public static List<? extends ExpressionTree> removeFirst(List<? extends ExpressionTree> arguments) {
        if (arguments.isEmpty()) {
            return arguments;
        }
        return arguments.subList(1, arguments.size());
    }

    public static Optional<ExpressionTree> matchAtIndex(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher, int index) {
        if (index < 0 || index > expressions.size() - 1) {
            return Optional.empty();
        }

        ExpressionTree candidateArgument = expressions.get(index);
        if (expressionTreeMatcher.matches(candidateArgument, state)) {
            return Optional.of(candidateArgument);
        }
        return Optional.empty();
    }

    public static boolean isStringType(ExpressionTree expressionTree, VisitorState state) {
        return STRING_MATCHER.matches(expressionTree, state);
    }

    public static boolean isStringLiteral(ExpressionTree expressionTree, VisitorState state) {
        return expressionTree instanceof LiteralTree && STRING_MATCHER.matches(expressionTree, state);
    }

    public static boolean isLoggerNamedAfterClass(ClassTree classTree, ExpressionTree argument, VisitorState state) {
        String expectedSimpleClassName = classTree.getSimpleName().toString();
        String expectedQualifiedClassName = ASTHelpers.getSymbol(classTree).fullname.toString();
        // case 1: getClass() on this specific class
        if ("getClass()".equals(argument.toString())) {
            return true;
        }

        // case 2: com.foo.X.class, where it matches current class
        if( staticFieldAccess().matches(argument,state) && isSameType("java.lang.Class" ).matches(argument,state) ) {
            if( expectedQualifiedClassName.equals( ASTHelpers.getSymbol(argument).owner.type.toString() ) ) {
                return true;
            }
        }
        // case 3: "com.foo.X", where it matches current class
        String stringValue = ASTHelpers.constValue(argument,String.class);
        if (expectedQualifiedClassName.equals(stringValue)) {
            return true;
        }

        // case 4: getClass().getName()
        if ("getClass().getName()".equals(argument.toString())) {
            return true;
        }

        // case 5: com.foo.X.class.getName()
        if(isClassGetName(argument, state) ) {
            return (expectedSimpleClassName + ".class.getName").equals(((MethodInvocationTree) argument).getMethodSelect().toString());
        }

        return false;
    }

    private static boolean isClassGetName(ExpressionTree argument, VisitorState state) {
        return methodInvocation(instanceMethod().onExactClass("java.lang.Class").named("getName")).matches(argument,state);
    }

    private Arguments() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }

}
