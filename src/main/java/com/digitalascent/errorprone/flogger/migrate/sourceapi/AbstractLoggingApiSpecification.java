package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.model.LogMessage;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.List;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public abstract class AbstractLoggingApiSpecification implements LoggingApiSpecification {

    private final Function<String, TargetLogLevel> targetLogLevelFunction;
    private final LogMessageModelFactory logMessageModelFactory;

    protected AbstractLoggingApiSpecification(Function<String, TargetLogLevel> targetLogLevelFunction,
                                           LogMessageModelFactory logMessageModelFactory) {
        this.targetLogLevelFunction = requireNonNull(targetLogLevelFunction, "targetLogLevelFunction");
        this.logMessageModelFactory = requireNonNull(logMessageModelFactory, "logMessageModelFactory");
    }

    protected final TargetLogLevel mapLogLevel(String level) {
        return targetLogLevelFunction.apply(level);
    }

    protected final LogMessage createLogMessageModel(ExpressionTree messageFormatArgument,
                                                     List<? extends ExpressionTree> remainingArguments,
                                                     VisitorState state,
                                                     @Nullable ExpressionTree thrownArgument,
                                                     MigrationContext migrationContext,
                                                     TargetLogLevel targetLogLevel) {
        return logMessageModelFactory.createLogMessageModel(messageFormatArgument, remainingArguments,
                state, thrownArgument, migrationContext, targetLogLevel);
    }
}
