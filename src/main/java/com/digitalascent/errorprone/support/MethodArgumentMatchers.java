package com.digitalascent.errorprone.support;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import java.util.Optional;

public final class MethodArgumentMatchers {
    public static Optional<ArgumentMatchResult> trailingArgument(MethodInvocationTree methodInvocationTree, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher) {
        return matchArgumentAtIndex(methodInvocationTree, state, expressionTreeMatcher, methodInvocationTree.getArguments().size() - 1 );
    }

    public Optional<ArgumentMatchResult> leading(MethodInvocationTree methodInvocationTree, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher) {
        return matchArgumentAtIndex(methodInvocationTree,state,expressionTreeMatcher,0);
    }

    public static Optional<ArgumentMatchResult> matchArgumentAtIndex(MethodInvocationTree methodInvocationTree, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher, int index ) {
        if( index < 0  || index > methodInvocationTree.getArguments().size() - 1 ) {
            return Optional.empty();
        }

        ExpressionTree candidateArgument = methodInvocationTree.getArguments().get(index);
        if (expressionTreeMatcher.matches(candidateArgument, state)) {
            return Optional.of(new ArgumentMatchResult(index, candidateArgument));
        }
        return Optional.empty();
    }

    public static Optional<ArgumentMatchResult> firstMatchingArgument(MethodInvocationTree methodInvocationTree, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher ) {
        for( int i = 0; i < methodInvocationTree.getArguments().size(); i++ ) {
            ExpressionTree candidateArgument = methodInvocationTree.getArguments().get(i);
            if (expressionTreeMatcher.matches(candidateArgument, state)) {
                return Optional.of(new ArgumentMatchResult(i, candidateArgument));
            }
        }
        return Optional.empty();
    }

    private MethodArgumentMatchers() {
        throw new AssertionError("Cannot instantiate " + getClass() );
    }
}
