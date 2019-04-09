package com.digitalascent.errorprone.flogger.migrate.format.reducer;

import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

import static com.google.errorprone.matchers.Matchers.instanceMethod;


/**
 * Removes toString() calls on message format parameters, allowing that to be deferred until it's determined
 * that the logging level is enabled
 */
public final class ToStringMessageFormatArgumentReducer implements MessageFormatArgumentReducer {
    private static final Matcher<ExpressionTree> TO_STRING_METHOD = instanceMethod()
            .onDescendantOf("java.lang.Object")
            .named("toString").withParameters();

    @Override
    public ExpressionTree reduce(ExpressionTree argument, VisitorState visitorState) {
        if( !TO_STRING_METHOD.matches(argument, visitorState)) {
            return null;
        }
        MethodInvocationTree methodInvocationTree = (MethodInvocationTree) argument;
        ExpressionTree select = methodInvocationTree.getMethodSelect();
        if( select instanceof JCTree.JCFieldAccess) {
            return ((JCTree.JCFieldAccess)select).selected;
        }
        return null;
    }
}
