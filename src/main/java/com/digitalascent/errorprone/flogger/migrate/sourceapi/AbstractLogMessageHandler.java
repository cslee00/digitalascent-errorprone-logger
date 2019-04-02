package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.matchers.method.MethodMatchers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;

import static com.google.errorprone.matchers.Matchers.isSubtypeOf;

public abstract class AbstractLogMessageHandler {
    private static final Matcher<ExpressionTree> STRING_MATCHER = isSubtypeOf(String.class);

    public final LogMessageModel processLogMessage(ExpressionTree messageFormatArgument,
                                                   List<? extends ExpressionTree> remainingArguments,
                                                   VisitorState state,
                                                   @Nullable ExpressionTree thrownArgument,
                                                   MigrationContext migrationContext) {
        LogMessageModel result1 = customProcessing(messageFormatArgument, state, thrownArgument);
        if( result1 != null ) {
            return result1;
        }

        if( thrownArgument == messageFormatArgument ) {
            // if only argument is a Throwable w/ no message, create a message
            return LogMessageModel.fromStringFormat("Exception", ImmutableList.of());
        }

        if (!STRING_MATCHER.matches(messageFormatArgument, state) ) {
            return LogMessageModel.fromStringFormat("%s", Arguments.prependArgument(remainingArguments, messageFormatArgument));
        }

        List<String> migrationIssues = new ArrayList<>();
        if (remainingArguments.isEmpty()) {
            // no arguments left after message format; check if message format argument is String.format
            LogMessageModel result = maybeUnpackStringFormat(messageFormatArgument, state );
            if( result != null ) {
                return result;
            }

            return LogMessageModel.fromMessageFormatArgument( messageFormatArgument, remainingArguments);
        }

        remainingArguments = Arguments.maybeUnpackVarArgs(remainingArguments,state);

        // handle remaining format arguments
        if (messageFormatArgument instanceof JCTree.JCLiteral && STRING_MATCHER.matches(messageFormatArgument,state)) {
            // handle common case of string literal format string
            String sourceMessageFormat = (String) ((JCTree.JCLiteral) messageFormatArgument).value;
            return convertMessageFormat(sourceMessageFormat, remainingArguments, migrationContext);
        }

        return LogMessageModel.unableToConvert(messageFormatArgument, remainingArguments);
    }

    @Nullable
    protected LogMessageModel customProcessing(ExpressionTree messageFormatArgument, VisitorState state, @Nullable ExpressionTree thrownArgument) {
        return null;
    }


    protected abstract LogMessageModel convertMessageFormat(String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext);

    private static final MethodMatchers.MethodNameMatcher STRING_FORMAT = Matchers.staticMethod().onClass("java.lang.String").named("format");

    @Nullable
    private LogMessageModel maybeUnpackStringFormat(ExpressionTree messageFormatArgument, VisitorState state ) {
        if (STRING_FORMAT.matches(messageFormatArgument, state)) {
            MethodInvocationTree stringFormatTree = (MethodInvocationTree) messageFormatArgument;
            ExpressionTree firstArgument = stringFormatTree.getArguments().get(0);
            if ((firstArgument instanceof JCTree.JCLiteral)) {
                String messageFormat = (String) ((JCTree.JCLiteral) firstArgument).value;
                return LogMessageModel.fromStringFormat(messageFormat, Arguments.removeFirst(stringFormatTree.getArguments()));
            }
        }

        return null;
    }

}
