package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.digitalascent.errorprone.flogger.migrate.format.MessageFormatArgument;
import com.digitalascent.errorprone.flogger.migrate.model.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatSpecification;
import com.google.common.base.CharMatcher;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class JULMessageFormatSpecification implements MessageFormatSpecification {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\{[0-9]})");
    private static final CharMatcher PARAM_PATTERN_DELIMITERS = CharMatcher.anyOf("{}");
    private static final com.google.errorprone.matchers.Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("java.util.function.Supplier")
    );

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
       return INVALID_MSG_FORMAT_TYPES.matches(messageFormatArgument,state);
    }

    @Override
    public LogMessageModel convertMessageFormat(String sourceMessageFormat, List<MessageFormatArgument> formatArguments, MigrationContext migrationContext) {

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

        return LogMessageModel.fromStringFormat(sb.toString(), argumentList, migrationIssues);
    }
}
