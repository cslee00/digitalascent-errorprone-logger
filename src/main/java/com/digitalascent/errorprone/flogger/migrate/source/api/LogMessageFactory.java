package com.digitalascent.errorprone.flogger.migrate.source.api;

import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.Arguments;
import com.digitalascent.errorprone.flogger.migrate.source.format.EmptyArgumentsHandler;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatConversionFailedException;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatConversionResult;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatEmptyArgumentsHandler;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.source.format.StringConcatenationEmptyArgumentsHandler;
import com.digitalascent.errorprone.flogger.migrate.source.format.StringFormatEmprtArgumentsHandler;
import com.digitalascent.errorprone.flogger.migrate.source.format.argconverter.MessageFormatArgumentConverter;
import com.digitalascent.errorprone.flogger.migrate.source.format.reducer.MessageFormatArgumentReducer;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static com.google.common.collect.ImmutableList.toImmutableList;
import static java.util.Objects.requireNonNull;

public final class LogMessageFactory {

    private final MessageFormatArgumentConverter messageFormatArgumentConverter;
    private final MessageFormatArgumentReducer messageFormatArgumentReducer;
    private final MessageFormatSpecification messageFormatSpecification;
    private final List<EmptyArgumentsHandler> emptyArgumentsHandlers;

    public LogMessageFactory(MessageFormatArgumentConverter messageFormatArgumentConverter,
                             MessageFormatArgumentReducer messageFormatArgumentReducer,
                             MessageFormatSpecification messageFormatSpecification) {
        this.messageFormatArgumentConverter = requireNonNull(messageFormatArgumentConverter, "messageFormatArgumentConverter");
        this.messageFormatArgumentReducer = requireNonNull(messageFormatArgumentReducer, "messageFormatArgumentReducer");
        this.messageFormatSpecification = requireNonNull(messageFormatSpecification, "messageFormatSpecification");

        emptyArgumentsHandlers = ImmutableList.<EmptyArgumentsHandler>builder()
                .add(new StringFormatEmprtArgumentsHandler())
                .add(new MessageFormatEmptyArgumentsHandler())
                .add(new StringConcatenationEmptyArgumentsHandler())
                .build();
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
            Optional<MessageFormatConversionResult> first = emptyArgumentsHandlers.stream()
                    .map(handler -> {
                        try {
                            return handler.handle(messageFormatArgument, state, targetLogLevel);
                        } catch( MessageFormatConversionFailedException e ) {
                            return null;
                        }
                    })
                    .filter(Objects::nonNull)
                    .findFirst();

            return first
                    .map(result -> {
                        List<? extends ExpressionTree> arguments = Arguments.maybeUnpackVarArgs(result.arguments(), state);
                        return LogMessage.fromStringFormat(result.messageFormat(), processArguments(arguments, state, targetLogLevel), result.conversionIssues());
                    })
                    .orElse(LogMessage.fromMessageFormatArgument(messageFormatArgument, ImmutableList.of()));
        }

        if (Arguments.isStringLiteral(messageFormatArgument, state)) {
            // handle common case of string literal format string
            String sourceMessageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;

            // convert from source message format (e.g. {} placeholders) to printf format specifiers
            MessageFormatConversionResult result = messageFormatSpecification.convertMessageFormat(messageFormatArgument, sourceMessageFormat, remainingArguments, migrationContext );
            return LogMessage.fromStringFormat( result.messageFormat(), processArguments(result.arguments(), state, targetLogLevel), result.conversionIssues());
        }

        return LogMessage.unableToConvert(messageFormatArgument, processArguments(remainingArguments, state, targetLogLevel));
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
