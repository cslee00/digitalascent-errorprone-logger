package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.google.common.base.CharMatcher;
import com.sun.source.tree.ExpressionTree;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JULMessageFormatConverter {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\{[0-9]})");
    private static final CharMatcher PARAM_PATTERN_DELIMITERS = CharMatcher.anyOf("{}");
    public static ConvertedMessageFormat convertMessageFormat(String messageFormat, List<? extends ExpressionTree> remainingArguments ) {
        List<ExpressionTree> argumentList = new ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(messageFormat);
        StringBuffer sb = new StringBuffer();
        while( matcher.find() ) {
            String text = matcher.group(1);
            text = PARAM_PATTERN_DELIMITERS.removeFrom(text);

            int index = Integer.parseInt(text);
            if( index < remainingArguments.size() ) {
                argumentList.add(remainingArguments.get(index));
            }
            matcher.appendReplacement(sb,"%s");
        }
        matcher.appendTail(sb);

        return new ConvertedMessageFormat(sb.toString(),argumentList);
    }

    public static final class ConvertedMessageFormat {
        private final String messageFormat;
        private final List<ExpressionTree> arguments;

        ConvertedMessageFormat(String messageFormat, List<ExpressionTree> arguments) {
            this.messageFormat = messageFormat;
            this.arguments = arguments;
        }

        String messageFormat() {
            return messageFormat;
        }

        List<ExpressionTree> arguments() {
            return arguments;
        }
    }
}
