package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.errorprone.matchers.Matchers.staticMethod;

final class MessageFormatEmptyArgumentsHandler implements EmptyArgumentsHandler {
    private static final MethodMatchers.MethodNameMatcher MESSAGE_FORMAT = staticMethod().onClass("java.text.MessageFormat").named("format");

    @Nullable
    @Override
    public MessageFormatConversionResult handle(ExpressionTree messageFormatArgument, VisitorState state, TargetLogLevel targetLogLevel) {
        if (!MESSAGE_FORMAT.matches(messageFormatArgument, state)) {
            return null;
        }

        MethodInvocationTree messageFormatTree = (MethodInvocationTree) messageFormatArgument;
        ExpressionTree firstArgument = messageFormatTree.getArguments().get(0);
        if (firstArgument instanceof LiteralTree) {
            String messageFormat = (String) ((LiteralTree) firstArgument).getValue();
            List<? extends ExpressionTree> remainingArguments = Arguments.removeFirst(messageFormatTree.getArguments());
            return MessageFormat.convertJavaTextMessageFormat( messageFormat, remainingArguments );
        }

        return null;
    }
}
