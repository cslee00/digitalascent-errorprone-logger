package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.collect.ImmutableList;
import com.sun.javafx.util.Logging;
import com.sun.source.tree.ExpressionTree;
import com.sun.source.tree.IfTree;
import com.sun.source.tree.MethodInvocationTree;

import java.util.List;

import static java.util.Objects.requireNonNull;

public final class LoggingConditional {
    private final IfTree ifTree;
    private final MethodInvocationTree loggingConditionalInvocation;
    private final LoggingConditionalActionType actionType;
    private final List<MethodInvocationTree> loggingMethods;

    static LoggingConditional migrateExpression( IfTree ifTree, MethodInvocationTree loggingConditionalInvocation ) {
        return new LoggingConditional( ifTree, loggingConditionalInvocation, LoggingConditionalActionType.MIGRATE_EXPRESSION_ONLY, ImmutableList.of());
    }

    static LoggingConditional elide( IfTree ifTree, MethodInvocationTree loggingConditionalInvocation ) {
        return elide( ifTree, loggingConditionalInvocation, ImmutableList.of());
    }

    static LoggingConditional elide( IfTree ifTree,  MethodInvocationTree loggingConditionalInvocation, List<MethodInvocationTree> loggingMethods ) {
        return new LoggingConditional(ifTree, loggingConditionalInvocation, LoggingConditionalActionType.ELIDE, loggingMethods);
    }
    private LoggingConditional(IfTree ifTree,
                       MethodInvocationTree loggingConditionalInvocation,
                       LoggingConditionalActionType actionType,
                       List<MethodInvocationTree> loggingMethods) {
        this.ifTree = requireNonNull(ifTree, "ifTree");
        this.loggingConditionalInvocation = requireNonNull(loggingConditionalInvocation, "loggingConditionalInvocation");
        this.actionType = requireNonNull(actionType, "actionType");
        this.loggingMethods = ImmutableList.copyOf(loggingMethods);
    }

    public IfTree ifTree() {
        return ifTree;
    }

    public MethodInvocationTree loggingConditionalInvocation() {
        return loggingConditionalInvocation;
    }

    public LoggingConditionalActionType actionType() {
        return actionType;
    }

    public List<MethodInvocationTree> loggingMethods() {
        return loggingMethods;
    }
}
