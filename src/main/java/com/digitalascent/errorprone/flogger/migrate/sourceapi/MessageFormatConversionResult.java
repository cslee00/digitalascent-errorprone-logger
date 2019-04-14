package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class MessageFormatConversionResult {
    private final String messageFormat;
    private final List<? extends ExpressionTree> arguments;
    private final List<String> conversionIssues;

    public MessageFormatConversionResult(String messageFormat, List<? extends ExpressionTree> arguments) {
        this( messageFormat, arguments, ImmutableList.of());
    }

    MessageFormatConversionResult(String messageFormat, List<? extends ExpressionTree> arguments, List<String> conversionIssues) {
        this.messageFormat = requireNonNull(messageFormat, "messageFormat");
        this.arguments = ImmutableList.copyOf(arguments);
        this.conversionIssues = ImmutableList.copyOf(conversionIssues);
    }

    String messageFormat() {
        return messageFormat;
    }

    public List<? extends ExpressionTree> arguments() {
        return arguments;
    }

    List<String> conversionIssues() {
        return conversionIssues;
    }
}
