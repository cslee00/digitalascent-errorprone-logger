package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import static com.google.errorprone.matchers.Matchers.isSameType;

/**
 */
public final class Log4j2MessageFormatArgumentConverter extends AbstractLazyArgConverter {

    @Override
    protected String decorate(String rawSource) {
        return "() -> " + rawSource + ".getFormattedMessage()";
    }

    @Override
    protected boolean matches(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        return isSameType("org.apache.logging.log4j.message.Message").matches(argument, visitorState);
    }
}
