package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.LogMessageModel;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.google.errorprone.VisitorState;
import com.sun.source.tree.ExpressionTree;

import javax.annotation.Nullable;
import java.util.List;

public interface LogMessageHandler {
    LogMessageModel processLogMessage(ExpressionTree messageFormatArgument,
                                      List<? extends ExpressionTree> remainingArguments,
                                      VisitorState state,
                                      @Nullable ExpressionTree thrownArgument,
                                      MigrationContext migrationContext,
                                      TargetLogLevel targetLogLevel);
}
