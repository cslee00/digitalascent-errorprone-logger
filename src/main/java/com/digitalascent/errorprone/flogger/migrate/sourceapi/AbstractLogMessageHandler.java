package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

public abstract class AbstractLogMessageHandler {
    private static final MethodMatchers.MethodNameMatcher STRING_FORMAT = Matchers.staticMethod().onClass("java.lang.String").named("format");
    private static final MessageFormatArgumentConverter MESSAGE_FORMAT_ARGUMENT_CONVERTER = new CompositeMessageFormatArgumentConverter();
    private static final MessageFormatArgumentReducer MESSAGE_FORMAT_ARGUMENT_REDUCER = new CompositeMessageFormatArgumentReducer();

    public final LogMessageModel processLogMessage(ExpressionTree messageFormatArgument,
                                                   List<? extends ExpressionTree> remainingArguments,
                                                   VisitorState state,
                                                   @Nullable ExpressionTree thrownArgument,
                                                   MigrationContext migrationContext) {

        if (shouldSkipMessageFormatArgument(messageFormatArgument, state)) {
            throw new SkipLogMethodException("Unable to convert message format: " + messageFormatArgument);
        }

        if (thrownArgument == messageFormatArgument) {
            // if only argument is a Throwable w/ no message, create a message
            return LogMessageModel.fromStringFormat("Exception", ImmutableList.of());
        }

        if (!Arguments.isStringType(messageFormatArgument, state)) {
            return LogMessageModel.fromStringFormat("%s",
                    processArguments(Arguments.prependArgument(remainingArguments, messageFormatArgument), state));
        }

        if (remainingArguments.isEmpty()) {
            // no arguments left after message format; check if message format argument is String.format
            LogMessageModel result = maybeUnpackStringFormat(messageFormatArgument, state);
            if (result != null) {
                return result;
            }

            return LogMessageModel.fromMessageFormatArgument(messageFormatArgument, processArguments(remainingArguments, state));
        }

        // handle remaining format arguments
        if (Arguments.isStringLiteral(messageFormatArgument, state)) {
            // handle common case of string literal format string
            String sourceMessageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;
            return convertMessageFormat(sourceMessageFormat, processArguments( remainingArguments, state ), migrationContext);
        }

        return LogMessageModel.unableToConvert(messageFormatArgument, processArguments(remainingArguments, state));
    }


    protected boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return false;
    }

    protected abstract LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext);

    @Nullable
    private LogMessageModel maybeUnpackStringFormat(ExpressionTree messageFormatArgument, VisitorState state) {
        if (STRING_FORMAT.matches(messageFormatArgument, state)) {
            MethodInvocationTree stringFormatTree = (MethodInvocationTree) messageFormatArgument;
            ExpressionTree firstArgument = stringFormatTree.getArguments().get(0);
            if ((firstArgument instanceof JCTree.JCLiteral)) {
                String messageFormat = (String) ((JCTree.JCLiteral) firstArgument).value;
                return LogMessageModel.fromStringFormat(messageFormat,
                        processArguments(Arguments.removeFirst(stringFormatTree.getArguments()), state));
            }
        }

        return null;
    }

    private List<MessageFormatArgument> processArguments(List<? extends ExpressionTree> arguments, VisitorState state) {
        return arguments.stream()
                .map( x -> MESSAGE_FORMAT_ARGUMENT_REDUCER.reduce(x,state) )
                .map(x -> MESSAGE_FORMAT_ARGUMENT_CONVERTER.convert(x,state))
                .collect(toImmutableList());
    }
}
