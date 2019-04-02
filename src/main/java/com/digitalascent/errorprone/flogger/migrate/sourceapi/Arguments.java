package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.support.MatchResult;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.digitalascent.errorprone.support.ExpressionMatchers.trailing;
import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

public final class Arguments {

    private static final Matcher<ExpressionTree> THROWABLE_MATCHER = isSubtypeOf(Throwable.class);

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
        if (remainingArguments.size() == 1) {
            ExpressionTree argument = remainingArguments.get(0);
            // if Object[] unpack
            if (Matchers.isArrayType().matches(argument, state)) {
                JCTree.JCNewArray newArray = (JCTree.JCNewArray) argument;
                return newArray.elems;
            }
        }

        return remainingArguments;
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
        if( arguments.isEmpty() ) {
            return arguments;
        }
        return arguments.subList(1, arguments.size());
    }

    public static List<? extends ExpressionTree> findMessageFormatArguments( List<? extends ExpressionTree> arguments, VisitorState state ) {
        List<? extends ExpressionTree> remainingArguments = removeFirst(arguments);
        return maybeUnpackVarArgs(remainingArguments, state);
    }
}
