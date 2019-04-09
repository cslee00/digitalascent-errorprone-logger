package com.digitalascent.errorprone.flogger.migrate.format.reducer;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;

import java.util.Arrays;

import static com.google.errorprone.matchers.Matchers.instanceMethod;
import static com.google.errorprone.matchers.Matchers.methodInvocation;
import static com.google.errorprone.matchers.Matchers.staticMethod;


public final class ArraysToStringMessageFormatArgumentReducer implements MessageFormatArgumentReducer {
    private static final Matcher<ExpressionTree> ARRAYS_TO_STRING_METHOD = methodInvocation(staticMethod()
            .onClass(Arrays.class.getName())
            .named("toString"));

    @Override
    public ExpressionTree reduce(ExpressionTree argument, VisitorState visitorState) {
        if( !ARRAYS_TO_STRING_METHOD.matches(argument, visitorState)) {
            return null;
        }
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) argument;
        if( methodInvocationTree.getArguments().size() != 1 ) {
            return null;
        }

        return methodInvocationTree.getArguments().get(0);
    }
}
