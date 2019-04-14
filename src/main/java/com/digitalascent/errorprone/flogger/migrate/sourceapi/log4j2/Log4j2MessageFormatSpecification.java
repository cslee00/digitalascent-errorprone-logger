package com.digitalascent.errorprone.flogger.migrate.sourceapi.log4j2;

import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatConversionResult;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatSpecification;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.MessageFormatStyle;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.tools.javac.code.Symbol;

import javax.annotation.Nullable;
import java.util.List;

public final class Log4j2MessageFormatSpecification implements MessageFormatSpecification {
    @Nullable
    private final MessageFormatStyle messageFormatStyle;

    private static final Matcher<ExpressionTree> INVALID_MSG_FORMAT_TYPES = Matchers.anyOf(
            Matchers.isSubtypeOf("org.apache.logging.log4j.message.Message"),
            Matchers.isSubtypeOf("org.apache.logging.log4j.util.MessageSupplier"),
            Matchers.isSubtypeOf("org.apache.logging.log4j.util.Supplier")
    );

    public Log4j2MessageFormatSpecification(@Nullable MessageFormatStyle messageFormatStyle) {
        this.messageFormatStyle = messageFormatStyle;
    }

    @Override
    public boolean shouldSkipMessageFormatArgument(ExpressionTree messageFormatArgument, VisitorState state) {
        return INVALID_MSG_FORMAT_TYPES.matches( messageFormatArgument, state);
    }

    @Override
    public MessageFormatConversionResult convertMessageFormat(ExpressionTree messageFormatArgument, String sourceMessageFormat, List<? extends ExpressionTree> formatArguments, MigrationContext migrationContext) {
        if( migrationContext.classNamedLoggers().isEmpty() && messageFormatStyle != null ) {
            // no logger variable definition (possibly from superclass or elsewhere); we can't accurately know
            // whether the logger was acquired via getLogger (brace-format) or getFormatter (printf-format)
            // use the (configurable) default
            switch( messageFormatStyle ) {
                case LOG4J2_BRACE:
                    String format = Log4j2BraceMessageFormatConverter.convertMessageFormat(sourceMessageFormat);
                    return new MessageFormatConversionResult(format, formatArguments );
                case PRINTF:
                    return new MessageFormatConversionResult(sourceMessageFormat, formatArguments);
            }
            throw new AssertionError("Unknown message format style: " + messageFormatStyle );
        }

        // single Log4j2 logger; if it is getLogger, convert the message format otherwise it's getFormatterLogger
        MethodInvocationTree logFactoryMethodInvocationTree = (MethodInvocationTree) migrationContext.classNamedLoggers().get(0).getInitializer();
        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(logFactoryMethodInvocationTree);
        String methodName = sym.getSimpleName().toString();
        if ("getLogger".equals(methodName)) {
            String format = Log4j2BraceMessageFormatConverter.convertMessageFormat(sourceMessageFormat);
            return new MessageFormatConversionResult(format, formatArguments);
        }
        // getFormatterLogger case, no need to convert message format as it's already printf-style
        return new MessageFormatConversionResult(sourceMessageFormat, formatArguments);
    }
}
