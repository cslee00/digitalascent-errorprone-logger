package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2;

import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.SkipLogMethodException;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.AbstractLogMessageHandler;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LogMessageModel;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;

import javax.annotation.Nullable;
import java.util.List;

final class Log4j2LogMessageHandler extends AbstractLogMessageHandler {
    private static final Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("org.apache.logging.log4j.message.Message"),
            Matchers.isSubtypeOf("org.apache.logging.log4j.util.MessageSupplier"),
            Matchers.isSubtypeOf("org.apache.logging.log4j.util.Supplier")
    );

    @Override
    protected boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return INVALID_MSG_FORMAT_TYPES.matches( messageFormatArgument, state);
    }

    @Override
    protected LogMessageModel convertMessageFormat(String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        return internalMessageFormat(sourceMessageFormat, formatArguments, migrationContext);
    }

    private LogMessageModel internalMessageFormat(String messageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        if (migrationContext.sourceLoggerMemberVariables().size() != 1) {
            // no existing variable definition (likely from a superclass or elsewhere)
            // assume default
            String format = Log4j2BraceMessageFormatConverter.convertMessageFormat(messageFormat);
            return LogMessageModel.fromStringFormat(format, formatArguments, ImmutableList.of("Unable to determine parameter format type; assuming default (brace '{}' style)"));
        }
        MethodInvocationTree logFactoryMethodInvocationTree = (MethodInvocationTree) migrationContext.sourceLoggerMemberVariables().get(0).getInitializer();
        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(logFactoryMethodInvocationTree);
        String methodName = sym.getSimpleName().toString();
        if ("getLogger".equals(methodName)) {
            String format = Log4j2BraceMessageFormatConverter.convertMessageFormat(messageFormat);
            return LogMessageModel.fromStringFormat(format, formatArguments);
        }
        return LogMessageModel.fromStringFormat(messageFormat, formatArguments);
    }
}
