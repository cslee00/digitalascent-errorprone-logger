package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.google.common.collect.ImmutableList;
import com.sun.source.tree.IfTree;

import java.util.List;

import static java.util.Objects.requireNonNull;

final class LoggingConditional {
    private final IfTree ifTree;
    private final MethodInvocation loggingConditionalInvocation;
    private final LoggingConditionalActionType actionType;
    private final List<MethodInvocation> loggingMethods;

    static LoggingConditional migrateExpression( IfTree ifTree, MethodInvocation loggingConditionalInvocation ) {
        return new LoggingConditional( ifTree, loggingConditionalInvocation, LoggingConditionalActionType.MIGRATE_EXPRESSION_ONLY, ImmutableList.of());
    }

    static LoggingConditional elide( IfTree ifTree, MethodInvocation loggingConditionalInvocation ) {
        return elide( ifTree, loggingConditionalInvocation, ImmutableList.of());
    }

    static LoggingConditional elide( IfTree ifTree,  MethodInvocation loggingConditionalInvocation, List<MethodInvocation> loggingMethods ) {
        return new LoggingConditional(ifTree, loggingConditionalInvocation, LoggingConditionalActionType.ELIDE, loggingMethods);
    }
    private LoggingConditional(IfTree ifTree,
                               MethodInvocation loggingConditionalInvocation,
                       LoggingConditionalActionType actionType,
                       List<MethodInvocation> loggingMethods) {
        this.ifTree = requireNonNull(ifTree, "ifTree");
        this.loggingConditionalInvocation = requireNonNull(loggingConditionalInvocation, "loggingConditionalInvocation");
        this.actionType = requireNonNull(actionType, "actionType");
        this.loggingMethods = ImmutableList.copyOf(loggingMethods);
    }

    IfTree ifTree() {
        return ifTree;
    }

    MethodInvocation loggingConditionalInvocation() {
        return loggingConditionalInvocation;
    }

    LoggingConditionalActionType actionType() {
        return actionType;
    }

    List<MethodInvocation> loggingMethods() {
        return loggingMethods;
    }
}
