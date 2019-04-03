package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.ToDoCommentGenerator;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.VariableTree;

import java.util.Optional;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

public abstract class AbstractLoggingApiConverter implements LoggingApiConverter {

    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;
    private final Function<String, TargetLogLevel> targetLogLevelFunction;

    public AbstractLoggingApiConverter(FloggerSuggestedFixGenerator floggerSuggestedFixGenerator, Function<String, TargetLogLevel> targetLogLevelFunction) {
        this.floggerSuggestedFixGenerator = requireNonNull(floggerSuggestedFixGenerator, "floggerSuggestedFixGenerator");
        this.targetLogLevelFunction = requireNonNull(targetLogLevelFunction, "targetLogLevelFunction");
    }

    protected abstract boolean matchLogFactory( VariableTree variableTree, VisitorState visitorState );

    @Override
    public final Optional<SuggestedFix> migrateLoggerVariable(ClassTree classTree, VariableTree variableTree,
                                                        VisitorState state, MigrationContext migrationContext) {
        checkArgument(isLoggerVariable(variableTree, state), "isLoggerVariable(variableTree, state) : %s", variableTree);

        if (!matchLogFactory(variableTree, state)) {
            return Optional.empty();
        }

        MethodInvocationTree logManagerMethodInvocationTree = (MethodInvocationTree) variableTree.getInitializer();
        if( logManagerMethodInvocationTree.getArguments().isEmpty() || Arguments.isLoggerNamedAfterClass(classTree, logManagerMethodInvocationTree.getArguments().get(0), state )) {
            return Optional.of(floggerSuggestedFixGenerator.generateLoggerVariable(classTree, variableTree, state, migrationContext));
        }

        String message = ToDoCommentGenerator.singleLineCommentForNode( "Unable to migrate logger variable", variableTree, state);
        SuggestedFix suggestedFix = SuggestedFix.builder()
                .prefixWith( variableTree, message )
                .build();
        return Optional.of(suggestedFix);
    }
}
