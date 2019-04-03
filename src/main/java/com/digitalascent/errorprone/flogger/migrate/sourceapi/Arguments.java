package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

import static com.google.errorprone.matchers.Matchers.instanceMethod;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.google.errorprone.matchers.Matchers.isSameType;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;
import static com.google.errorprone.matchers.Matchers.staticFieldAccess;

public final class Arguments {
    private static final Matcher<ExpressionTree> STRING_MATCHER = isSubtypeOf(String.class);
    private static final Matcher<ExpressionTree> THROWABLE_MATCHER = isSubtypeOf(Throwable.class);
    private static final MethodMatchers.MethodNameMatcher STRING_FORMAT = Matchers.staticMethod().onClass("java.lang.String").named("format");

    public static List<? extends ExpressionTree> removeLast(List<? extends ExpressionTree> expressions) {
        if (expressions.size() <= 1) {
            return ImmutableList.of();
        }
        return expressions.subList(0, expressions.size() - 1);
    }

    @Nullable
    public static ExpressionTree findTrailingThrowable(List<? extends ExpressionTree> remainingArguments, VisitorState state) {
        Optional<MatchResult> matchResult = trailing(remainingArguments, state, THROWABLE_MATCHER);
        return matchResult.map(MatchResult::argument).orElse(null);
    }

    public static List<? extends ExpressionTree> findRemainingAfter(List<? extends ExpressionTree> arguments, VisitorState state, ExpressionTree after) {
        List<ExpressionTree> remainingArguments = new ArrayList<>();
        boolean acquire = false;
        for (ExpressionTree argument : arguments) {
            if (argument == after) {
                acquire = true;
                continue;
            }
            if (!acquire) {
                continue;
            }
            remainingArguments.add(argument);
        }
        return maybeUnpackVarArgs(remainingArguments, state);
    }

    private static List<? extends ExpressionTree> maybeUnpackVarArgs(List<? extends ExpressionTree> arguments, VisitorState state) {
        if (arguments.size() == 1) {
            ExpressionTree argument = arguments.get(0);
            // if Object[] unpack
            if (Matchers.isArrayType().matches(argument, state)) {
                JCTree.JCNewArray newArray = (JCTree.JCNewArray) argument;
                return newArray.elems;
            }
        }

        return arguments;
    }

    private Arguments() {
        throw new AssertionError("Cannot instantiate " + getClass());
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

    public static List<? extends ExpressionTree> findMessageFormatArguments(List<? extends ExpressionTree> arguments, VisitorState state) {
        List<? extends ExpressionTree> remainingArguments = removeFirst(arguments);
        return maybeUnpackVarArgs(remainingArguments, state);
    }

    private static Optional<MatchResult> trailing(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher) {
        return matchAtIndex(expressions, state, expressionTreeMatcher, expressions.size() - 1);
    }

    public static Optional<MatchResult> matchAtIndex(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher, int index) {
        if (index < 0 || index > expressions.size() - 1) {
            return Optional.empty();
        }

        ExpressionTree candidateArgument = expressions.get(index);
        if (expressionTreeMatcher.matches(candidateArgument, state)) {
            return Optional.of(new MatchResult(index, candidateArgument));
        }
        return Optional.empty();
    }

    public static Optional<MatchResult> firstMatching(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher) {
        for (int i = 0; i < expressions.size(); i++) {
            ExpressionTree candidate = expressions.get(i);
            if (expressionTreeMatcher.matches(candidate, state)) {
                return Optional.of(new MatchResult(i, candidate));
            }
        }
        return Optional.empty();
    }

    public static LogMessageFormatSpec maybeUnpackStringFormat(ExpressionTree messageFormatArgument, VisitorState state) {
        if (STRING_FORMAT.matches(messageFormatArgument, state)) {
            MethodInvocationTree stringFormatTree = (MethodInvocationTree) messageFormatArgument;
            ExpressionTree firstArgument = stringFormatTree.getArguments().get(0);
            if ((firstArgument instanceof JCTree.JCLiteral)) {
                String messageFormat = (String) ((JCTree.JCLiteral) firstArgument).value;
                return new LogMessageFormatSpec(messageFormat, Arguments.removeFirst(stringFormatTree.getArguments()));
            }
        }

        return null;
    }

    static boolean isStringType(ExpressionTree expressionTree, VisitorState state) {
        return STRING_MATCHER.matches(expressionTree, state);
    }

    static boolean isStringLiteral(ExpressionTree expressionTree, VisitorState state) {
        return expressionTree instanceof JCTree.JCLiteral && STRING_MATCHER.matches(expressionTree, state);
    }

    public static boolean isLoggerNamedAfterClass(ClassTree classTree, ExpressionTree argument, VisitorState state) {
        String expectedClassName = ASTHelpers.getSymbol(classTree).fullname.toString();
        // case 1: getClass() on this specific class
        if ("getClass()".equals(argument.toString())) {
            return true;
        }

        // case 2: com.foo.X.class, where it matches current class
        if ((expectedClassName + ".class").equals(argument.toString())) {
            return true;
        }

        // case 3: "com.foo.X", where it matches current class
        if (expectedClassName.equals(argument.toString())) {
            return true;
        }

        // case 4: getClass().getName()
        if ("getClass().getName()".equals(argument.toString())) {
            return true;
        }

        return false;
    }

    public static final class LogMessageFormatSpec {
        private final String formatString;
        private final List<? extends ExpressionTree> arguments;

        private LogMessageFormatSpec(String formatString, List<? extends ExpressionTree> arguments) {
            this.formatString = formatString;
            this.arguments = ImmutableList.copyOf(arguments);
        }

        public String formatString() {
            return formatString;
        }

        public List<? extends ExpressionTree> arguments() {
            return arguments;
        }
    }
}
