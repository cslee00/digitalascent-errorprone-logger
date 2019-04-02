package com.digitalascent.errorprone.support;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;

import java.util.List;
import java.util.Optional;

public final class ExpressionMatchers {
    public static Optional<MatchResult> trailing(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher) {
        return matchAtIndex(expressions, state, expressionTreeMatcher, expressions.size() - 1 );
    }

    public Optional<MatchResult> leading(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher) {
        return matchAtIndex(expressions,state,expressionTreeMatcher,0);
    }

    public static Optional<MatchResult> matchAtIndex(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher, int index ) {
        if( index < 0  || index > expressions.size() - 1 ) {
            return Optional.empty();
        }

        ExpressionTree candidateArgument = expressions.get(index);
        if (expressionTreeMatcher.matches(candidateArgument, state)) {
            return Optional.of(new MatchResult(index, candidateArgument));
        }
        return Optional.empty();
    }

    public static Optional<MatchResult> firstMatching(List<? extends ExpressionTree> expressions, VisitorState state, Matcher<ExpressionTree> expressionTreeMatcher ) {
        for( int i = 0; i < expressions.size(); i++ ) {
            ExpressionTree candidate = expressions.get(i);
            if (expressionTreeMatcher.matches(candidate, state)) {
                return Optional.of(new MatchResult(i, candidate));
            }
        }
        return Optional.empty();
    }

    private ExpressionMatchers() {
        throw new AssertionError("Cannot instantiate " + getClass() );
    }
}
