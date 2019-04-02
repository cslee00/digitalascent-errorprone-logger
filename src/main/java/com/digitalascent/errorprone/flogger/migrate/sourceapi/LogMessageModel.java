package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.List;

import static java.util.Objects.requireNonNull;

public final class LogMessageModel {
    @Nullable
    private final String messageFormat;
    private final List<? extends ExpressionTree> arguments;
    private final List<String> migrationIssues;

    @Nullable
    private final ExpressionTree messageFormatArgument;

    public static LogMessageModel unableToConvert(ExpressionTree messageFormatArgument, List<? extends ExpressionTree> arguments) {
        requireNonNull(messageFormatArgument, "messageFormatArgument");
        return new LogMessageModel(null, arguments, ImmutableList.of("Unable to convert message format expression - not a string literal"), messageFormatArgument);
    }

    public static LogMessageModel fromMessageFormatArgument(ExpressionTree messageFormatArgument, List<? extends ExpressionTree> arguments) {
        requireNonNull(messageFormatArgument, "messageFormatArgument");
        requireNonNull(arguments, "arguments");
        return new LogMessageModel(null, arguments, ImmutableList.of(), messageFormatArgument);
    }

    public static LogMessageModel fromStringFormat(String messageFormat, List<? extends ExpressionTree> arguments, List<String> migrationIssues) {
        requireNonNull(messageFormat, "messageFormat");
        requireNonNull(arguments, "arguments");
        return new LogMessageModel(messageFormat, arguments, migrationIssues, null);
    }

    public static LogMessageModel fromStringFormat(String messageFormat, List<? extends ExpressionTree> arguments) {
        return fromStringFormat(messageFormat, arguments, ImmutableList.of());
    }


    private LogMessageModel(@Nullable String messageFormat, List<? extends ExpressionTree> arguments, List<String> migrationIssues, @Nullable ExpressionTree messageFormatArgument) {
        this.messageFormat = messageFormat;
        this.arguments = arguments;
        this.migrationIssues = ImmutableList.copyOf(migrationIssues);
        this.messageFormatArgument = messageFormatArgument;
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

    public List<? extends ExpressionTree> arguments() {
        return arguments;
    }
}
