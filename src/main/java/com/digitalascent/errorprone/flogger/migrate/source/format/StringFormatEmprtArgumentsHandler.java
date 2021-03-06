package com.digitalascent.errorprone.flogger.migrate.source.format;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.Arguments;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.errorprone.matchers.Matchers.staticMethod;

/**
 * Unpacks String.format calls
 */
public final class StringFormatEmprtArgumentsHandler implements EmptyArgumentsHandler {
    private static final MethodMatchers.MethodNameMatcher STRING_FORMAT = staticMethod().onClass("java.lang.String").named("format");

    @Nullable
    @Override
    public MessageFormatConversionResult handle(ExpressionTree messageFormatArgument, VisitorState state, TargetLogLevel targetLogLevel) {
        if (!STRING_FORMAT.matches(messageFormatArgument, state)) {
            return null;
        }

        MethodInvocationTree stringFormatTree = (MethodInvocationTree) messageFormatArgument;
        ExpressionTree firstArgument = stringFormatTree.getArguments().get(0);
        if (firstArgument instanceof LiteralTree) {
            String messageFormat = (String) ((LiteralTree) firstArgument).getValue();
            List<? extends ExpressionTree> remainingArguments = Arguments.removeFirst(stringFormatTree.getArguments());
            return new MessageFormatConversionResult(messageFormat, remainingArguments);
        }

        return null;
    }
}
