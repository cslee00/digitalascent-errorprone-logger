package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.ExpressionTree;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JULMessageFormatConverter {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\{[0-9]})");
    private static final CharMatcher PARAM_PATTERN_DELIMITERS = CharMatcher.anyOf("{}");
    static ConvertedMessageFormat convertMessageFormat(String messageFormat, List<? extends ExpressionTree> remainingArguments) {
        List<ExpressionTree> argumentList = new ArrayList<>();
        List<String> migrationIssues = new ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(messageFormat);
        StringBuffer sb = new StringBuffer();
        while( matcher.find() ) {
            String text = matcher.group(1);
            text = PARAM_PATTERN_DELIMITERS.removeFrom(text);

            int index = Integer.parseInt(text);
            if( index < remainingArguments.size() ) {
                argumentList.add(remainingArguments.get(index));
                matcher.appendReplacement(sb,"%s");
            } else {
                migrationIssues.add( "Invalid parameter index: " + matcher.group(1) + ": \"" + messageFormat + "\"");
            }
        }
        matcher.appendTail(sb);

        for (ExpressionTree remainingArgument : remainingArguments) {
            if( !argumentList.contains(remainingArgument)) {
                argumentList.add(remainingArgument);
            }
        }

        return new ConvertedMessageFormat(sb.toString(),argumentList, migrationIssues);
    }

    static final class ConvertedMessageFormat {
        private final String messageFormat;
        private final List<ExpressionTree> arguments;
        private final List<String> migrationIssues;

        ConvertedMessageFormat(String messageFormat, List<ExpressionTree> arguments, List<String> migrationIssues) {
            this.messageFormat = messageFormat;
            this.arguments = arguments;
            this.migrationIssues = ImmutableList.copyOf(migrationIssues);
        }

        public List<String> migrationIssues() {
            return migrationIssues;
        }

        String messageFormat() {
            return messageFormat;
        }

        List<ExpressionTree> arguments() {
            return arguments;
        }
    }
}
