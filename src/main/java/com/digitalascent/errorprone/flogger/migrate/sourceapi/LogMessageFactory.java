package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.format.converter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.format.reducer.MessageFormatArgumentReducer;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LiteralTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static com.google.errorprone.matchers.Matchers.staticMethod;
import static java.util.Objects.requireNonNull;

public final class LogMessageFactory {
    private static final MethodMatchers.MethodNameMatcher STRING_FORMAT = staticMethod().onClass("java.lang.String").named("format");
    private static final MethodMatchers.MethodNameMatcher MESSAGE_FORMAT = staticMethod().onClass("java.text.MessageFormat").named("format");

    private final MessageFormatArgumentConverter messageFormatArgumentConverter;
    private final MessageFormatArgumentReducer messageFormatArgumentReducer;
    private final MessageFormatSpecification messageFormatSpecification;

    public LogMessageFactory(MessageFormatArgumentConverter messageFormatArgumentConverter,
                             MessageFormatArgumentReducer messageFormatArgumentReducer,
                             MessageFormatSpecification messageFormatSpecification) {
        this.messageFormatArgumentConverter = requireNonNull(messageFormatArgumentConverter, "messageFormatArgumentConverter");
        this.messageFormatArgumentReducer = requireNonNull(messageFormatArgumentReducer, "messageFormatArgumentReducer");
        this.messageFormatSpecification = requireNonNull(messageFormatSpecification, "messageFormatSpecification");
    }

    final LogMessage create(ExpressionTree messageFormatArgument,
                            List<? extends ExpressionTree> remainingArguments,
                            VisitorState state,
                            @Nullable ExpressionTree thrownArgument,
                            MigrationContext migrationContext,
                            TargetLogLevel targetLogLevel) {

        if (messageFormatSpecification.shouldSkipMessageFormatArgument(messageFormatArgument, state)) {
            throw new SkipLogMethodException("Unable to convert message format: " + messageFormatArgument);
        }

        if (thrownArgument == messageFormatArgument) {
            // if only extract is a Throwable w/ no message, create a message
            return LogMessage.fromStringFormat("Exception", ImmutableList.of());
        }

        if (!Arguments.isStringType(messageFormatArgument, state)) {
            // message format extract is some other type (some loggers allow Object for the format)
            // emit as "%s", arg
            return LogMessage.fromStringFormat("%s",
                    processArguments(Arguments.prependArgument(remainingArguments, messageFormatArgument), state, targetLogLevel));
        }

        if (remainingArguments.isEmpty()) {
            // no arguments left after message format; check if message format extract is String.format
            LogMessage result = maybeUnpackStringFormat(messageFormatArgument, state, targetLogLevel);
            if (result != null) {
                return result;
            }

            result = maybeUnpackMessageFormat(messageFormatArgument,state,targetLogLevel);
            if( result != null ) {
                return result;
            }

            return LogMessage.fromMessageFormatArgument(messageFormatArgument, processArguments(remainingArguments, state, targetLogLevel));
        }

        if (Arguments.isStringLiteral(messageFormatArgument, state)) {
            // handle common case of string literal format string
            String sourceMessageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;

            // convert from source message format (e.g. {} placeholders) to printf format specifiers
            return messageFormatSpecification.convertMessageFormat(messageFormatArgument, sourceMessageFormat, processArguments(remainingArguments, state, targetLogLevel), migrationContext);
        }

        return LogMessage.unableToConvert(messageFormatArgument, processArguments(remainingArguments, state, targetLogLevel));
    }

    private LogMessage maybeUnpackMessageFormat(ExpressionTree messageFormatArgument, VisitorState state, TargetLogLevel targetLogLevel) {
        if (MESSAGE_FORMAT.matches(messageFormatArgument, state)) {
            MethodInvocationTree messageFormatTree = (MethodInvocationTree) messageFormatArgument;
            ExpressionTree firstArgument = messageFormatTree.getArguments().get(0);
            if (firstArgument instanceof LiteralTree) {
                String messageFormat = (String) ((LiteralTree) firstArgument).getValue();
                List<? extends ExpressionTree> remainingArguments = Arguments.removeFirst(messageFormatTree.getArguments());
                remainingArguments = Arguments.maybeUnpackVarArgs(remainingArguments, state);
                return MessageFormat.convertJavaTextMessageFormat(messageFormatArgument, messageFormat, processArguments(remainingArguments, state, targetLogLevel) );
            }
        }

        return null;
    }

    @Nullable
    private LogMessage maybeUnpackStringFormat(ExpressionTree messageFormatArgument, VisitorState state, TargetLogLevel targetLogLevel) {
        if (STRING_FORMAT.matches(messageFormatArgument, state)) {
            MethodInvocationTree stringFormatTree = (MethodInvocationTree) messageFormatArgument;
            ExpressionTree firstArgument = stringFormatTree.getArguments().get(0);
            if (firstArgument instanceof LiteralTree) {
                String messageFormat = (String) ((LiteralTree) firstArgument).getValue();
                List<? extends  ExpressionTree> remainingArguments = Arguments.removeFirst(stringFormatTree.getArguments());
                remainingArguments = Arguments.maybeUnpackVarArgs(remainingArguments, state);
                return LogMessage.fromStringFormat(messageFormat,
                        processArguments(remainingArguments, state, targetLogLevel));
            }
        }

        return null;
    }

    private List<MessageFormatArgument> processArguments(List<? extends ExpressionTree> arguments,
                                                         VisitorState state,
                                                         TargetLogLevel targetLogLevel) {
        return arguments.stream()
                .map(x -> messageFormatArgumentReducer.reduce(x, state))
                .map(x -> messageFormatArgumentConverter.convert(x, state, targetLogLevel))
                .collect(toImmutableList());
    }
}