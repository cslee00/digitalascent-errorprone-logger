package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.google.common.base.CharMatcher;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class MessageFormat {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\{[0-9]})");
    private static final CharMatcher PARAM_PATTERN_DELIMITERS = CharMatcher.anyOf("{}");

    // TODO - handle conversion of single quotes
    // TODO - handle escaping of percent signs
    public static LogMessage convertJavaTextMessageFormat(String sourceMessageFormat,
                                                          List<MessageFormatArgument> formatArguments) {

        List<MessageFormatArgument> argumentList = new ArrayList<>();
        List<String> migrationIssues = new ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(sourceMessageFormat);
        StringBuffer sb = new StringBuffer();
        while( matcher.find() ) {
            String text = matcher.group(1);
            text = PARAM_PATTERN_DELIMITERS.removeFrom(text);

            int index = Integer.parseInt(text);
            if( index < formatArguments.size() ) {
                argumentList.add(formatArguments.get(index));
                matcher.appendReplacement(sb,"%s");
            } else {
                migrationIssues.add( "Invalid parameter index: " + matcher.group(1) + ": \"" + sourceMessageFormat + "\"");
            }
        }
        matcher.appendTail(sb);

        for (MessageFormatArgument remainingArgument : formatArguments) {
            if( !argumentList.contains(remainingArgument)) {
                argumentList.add(remainingArgument);
            }
        }

        return LogMessage.fromStringFormat(sb.toString(), argumentList, migrationIssues);
    }

    private MessageFormat() {
        throw new AssertionError("Cannot instantiate: " + getClass() );
    }
}
