package com.digitalascent.errorprone.flogger.migrate.model;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Objects.requireNonNull;

/**
 * Represents a log message - the format, arguments and any migration notes
 */
public final class LogMessageModel {
    @Nullable
    private final String messageFormat;
    private final List<MessageFormatArgument> arguments;
    private final List<String> migrationIssues;

    @Nullable
    private final ExpressionTree messageFormatArgument;

    public static LogMessageModel unableToConvert(ExpressionTree messageFormatArgument, List<MessageFormatArgument> arguments) {
        return new LogMessageModel(null, arguments, ImmutableList.of("Unable to convert message format expression - not a string literal"), messageFormatArgument);
    }

    public static LogMessageModel fromMessageFormatArgument(ExpressionTree messageFormatArgument, List<MessageFormatArgument> arguments) {
        return new LogMessageModel(null, arguments, ImmutableList.of(), messageFormatArgument);
    }

    public static LogMessageModel fromStringFormat(String messageFormat, List<MessageFormatArgument> arguments, List<String> migrationIssues) {
        return new LogMessageModel(messageFormat, arguments, migrationIssues, null);
    }

    public static LogMessageModel fromStringFormat(String messageFormat, List<MessageFormatArgument> arguments) {
        return fromStringFormat(messageFormat, arguments, ImmutableList.of());
    }

    private LogMessageModel(@Nullable String messageFormat, List<MessageFormatArgument> arguments, List<String> migrationIssues, @Nullable ExpressionTree messageFormatArgument) {
        this.messageFormat = messageFormatArgument == null ? requireNonNull(messageFormat, "messageFormat") : messageFormat;
        this.arguments = ImmutableList.copyOf(arguments);
        this.migrationIssues = ImmutableList.copyOf(migrationIssues);
        this.messageFormatArgument = messageFormat == null ? requireNonNull(messageFormatArgument) : messageFormatArgument;
    }

    @Nullable
    public ExpressionTree messageFormatArgument() {
        return messageFormatArgument;
    }

    public List<String> migrationIssues() {
        return migrationIssues;
    }

    @Nullable
    public String messageFormat() {
        return messageFormat;
    }

    public List<MessageFormatArgument> arguments() {
        return arguments;
    }
}
