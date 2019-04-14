package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.google.common.base.Verify;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static java.util.Objects.requireNonNull;

public final class ArgumentParser {

    private List<? extends ExpressionTree> arguments;
    private final MethodInvocation methodInvocation;

    private ArgumentParser(MethodInvocation methodInvocation) {
        this.methodInvocation = requireNonNull(methodInvocation, "methodInvocation");
        this.arguments = new ArrayList<>( methodInvocation.tree().getArguments() );
    }

    public static ArgumentParser forArgumentsOf(MethodInvocation methodInvocation) {
        return new ArgumentParser(methodInvocation);
    }

    public void skipIfPresent(Predicate<ExpressionTree> predicate ) {
        if( predicate.test( currentArgument() )) {
            nextArgument();
        }
    }

    private void nextArgument() {
        arguments = Arguments.removeFirst( arguments );
    }

    private ExpressionTree currentArgument() {
        return arguments.get(0);
    }

    public ExpressionTree extractIfMatches( Predicate<ExpressionTree> predicate ) {
        ExpressionTree expressionTree = currentArgument();
        if( predicate.test( expressionTree )) {
            nextArgument();
            return expressionTree;
        }
        return null;
    }

    public ExpressionTree extract() {
        return extractIfMatches(x -> true );
    }

    public void maybeUnpackVarArgs() {
        arguments = Arguments.maybeUnpackVarArgs(arguments, methodInvocation.state());
    }

    @Nullable
    public ExpressionTree trailingThrowable() {
        ExpressionTree throwable = Arguments.findTrailingThrowable(arguments, methodInvocation.state());
        if( throwable != null ) {
            removeLast();
        }
        return throwable;
    }

    private void removeLast() {
        arguments = Arguments.removeLast(arguments);
    }

    public List<? extends ExpressionTree> remainingArguments() {
        return ImmutableList.copyOf(arguments);
    }

    public ExpressionTree extractOrElse(@Nullable ExpressionTree expressionTree) {
        if( arguments.isEmpty() ) {
            return expressionTree;
        }
        ExpressionTree argument = currentArgument();
        nextArgument();

        return argument;
    }

    public ExpressionTree firstMatching(Predicate<ExpressionTree> expressionTreePredicate ) {
        if( arguments.isEmpty()) {
           throw new IllegalStateException("Unable to locate argument");
        }
        ExpressionTree expressionTree = currentArgument();
        if( expressionTreePredicate.test( expressionTree )) {
            nextArgument();
            return expressionTree;
        }
        return firstMatching(expressionTreePredicate);
    }
}
