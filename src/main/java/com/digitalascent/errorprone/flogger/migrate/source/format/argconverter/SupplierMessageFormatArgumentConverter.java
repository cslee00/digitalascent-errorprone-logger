package com.digitalascent.errorprone.flogger.migrate.source.format.argconverter;

import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatArgument;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matchers;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.LambdaExpressionTree;
import com.sun.source.tree.NewArrayTree;
import com.sun.source.tree.NewClassTree;
import com.sun.tools.javac.code.Type;

import static com.google.errorprone.matchers.Matchers.anyMethod;

/**
 */
public final class SupplierMessageFormatArgumentConverter implements MessageFormatArgumentConverter {

    private static final ImmutableList<String> LAZY_ARG_IMPORT = ImmutableList.of("com.google.common.flogger.LazyArgs.lazy");

    @Override
    public MessageFormatArgument convert(ExpressionTree argument, VisitorState visitorState, TargetLogLevel targetLogLevel) {
        if (isSupplierArgument(argument, visitorState)) {
            String rawSource = visitorState.getSourceForNode(argument);
            if (rawSource == null) {
                return null;
            }
            String source = "lazy(" + rawSource + ")";
            return MessageFormatArgument.fromCode(source, ImmutableList.of(), LAZY_ARG_IMPORT);
        }
        return null;
    }

    private boolean isSupplierArgument(ExpressionTree argument, VisitorState visitorState) {
        if( !(argument instanceof LambdaExpressionTree)) {
            return false;
        }

        LambdaExpressionTree lambdaExpressionTree = (LambdaExpressionTree) argument;
        Type type = ASTHelpers.getType(lambdaExpressionTree);
        if( type == null || !type.tsym.toString().equals("java.util.function.Supplier") ) {
            return false;
        }
//
//        if( type.getParameterTypes().isEmpty() ) {
//            return false;
//        }

        return true;
    }
}
