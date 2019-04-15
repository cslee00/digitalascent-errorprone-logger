package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;

/**
 */
public final class LambdaMessageFormatArgumentConverter extends AbstractLazyArgConverter {

    @Override
    protected String decorate(String rawSource) {
        return rawSource;
    }

    @Override
    protected boolean matches(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        return argument instanceof LambdaExpressionTree;
    }
}
