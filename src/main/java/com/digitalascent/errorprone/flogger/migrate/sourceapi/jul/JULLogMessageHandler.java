package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
import com.google.common.base.CharMatcher;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JULLogMessageHandler extends AbstractLogMessageHandler {

    private static final Pattern PARAM_PATTERN = Pattern.compile("(\\{[0-9]})");
    private static final CharMatcher PARAM_PATTERN_DELIMITERS = CharMatcher.anyOf("{}");
    private static final com.google.errorprone.matchers.Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("java.util.function.Supplier")
    );

    @Nullable
    @Override
    protected LogMessageModel customProcessing(ExpressionTree messageFormatArgument, VisitorState state, @Nullable ExpressionTree thrownArgument) {
        if(INVALID_MSG_FORMAT_TYPES.matches(messageFormatArgument,state)) {
            throw new SkipLogMethodException("Unable to convert message format: " + messageFormatArgument);
        }
        return null;
    }

    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return doConvert(sourceMessageFormat, formatArguments);
    }

    private LogMessageModel doConvert(String messageFormat, List<? extends ExpressionTree> formatArguments) {
        List<ExpressionTree> argumentList = new ArrayList<>();
        List<String> migrationIssues = new ArrayList<>();
        Matcher matcher = PARAM_PATTERN.matcher(messageFormat);
        StringBuffer sb = new StringBuffer();
        while( matcher.find() ) {
            String text = matcher.group(1);
            text = PARAM_PATTERN_DELIMITERS.removeFrom(text);

            int index = Integer.parseInt(text);
            if( index < formatArguments.size() ) {
                argumentList.add(formatArguments.get(index));
                matcher.appendReplacement(sb,"%s");
            } else {
                migrationIssues.add( "Invalid parameter index: " + matcher.group(1) + ": \"" + messageFormat + "\"");
            }
        }
        matcher.appendTail(sb);

        for (ExpressionTree remainingArgument : formatArguments) {
            if( !argumentList.contains(remainingArgument)) {
                argumentList.add(remainingArgument);
            }
        }

        return LogMessageModel.fromStringFormat(sb.toString(), argumentList, migrationIssues);
    }
}
