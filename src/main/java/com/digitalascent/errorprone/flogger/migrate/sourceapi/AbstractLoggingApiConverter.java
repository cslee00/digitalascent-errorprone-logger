package com.digitalascent.errorprone.flogger.migrate.sourceapi;

import com.digitalascent.errorprone.flogger.migrate.FloggerSuggestedFixGenerator;
import com.digitalascent.errorprone.flogger.migrate.ImmutableFloggerLogContext;
import com.digitalascent.errorprone.flogger.migrate.LoggingApiConverter;
import com.digitalascent.errorprone.flogger.migrate.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.TargetLogLevel;
import com.digitalascent.errorprone.flogger.migrate.ToDoCommentGenerator;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

import java.util.Optional;
import java.util.Set;
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


    @Override
    public final Optional<SuggestedFix> migrateImport(ImportTree importTree, VisitorState visitorState) {
        if (matchImport(importTree.getQualifiedIdentifier(), visitorState)) {
            return Optional.of(floggerSuggestedFixGenerator.removeImport(importTree));
        }

        if( loggingPackagePrefixes().stream()
                .anyMatch( x -> importTree.getQualifiedIdentifier().toString().startsWith(x)) ) {
            return Optional.of(floggerSuggestedFixGenerator.removeImport(importTree));
        }
        return Optional.empty();
    }

    @Override
    public final Optional<SuggestedFix> migrateLoggerVariable(ClassTree classTree, VariableTree variableTree,
                                                              VisitorState state, MigrationContext migrationContext) {
        checkArgument(isLoggerVariable(variableTree, state), "isLoggerVariable(variableTree, state) : %s", variableTree);

        if (!matchLogFactory(variableTree, state)) {
            return Optional.empty();
        }

        MethodInvocationTree logManagerMethodInvocationTree = (MethodInvocationTree) variableTree.getInitializer();
        if (logManagerMethodInvocationTree.getArguments().isEmpty() || Arguments.isLoggerNamedAfterClass(classTree, logManagerMethodInvocationTree.getArguments().get(0), state)) {
            return Optional.of(floggerSuggestedFixGenerator.generateLoggerVariable(classTree, variableTree, state, migrationContext));
        }

        String message = ToDoCommentGenerator.singleLineCommentForNode("Unable to migrate logger variable", variableTree, state);
        SuggestedFix suggestedFix = SuggestedFix.builder()
                .prefixWith(variableTree, message)
                .build();
        return Optional.of(suggestedFix);
    }

    @Override
    public final Optional<SuggestedFix> migrateLoggingMethodInvocation(MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext) {

        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(methodInvocationTree);
        String methodName = sym.getSimpleName().toString();
        if (matchLoggingMethod(methodInvocationTree, state)) {
            ImmutableFloggerLogContext floggerLogContext = migrateLoggingMethod(methodName, methodInvocationTree, state, migrationContext);
            return Optional.of(getFloggerSuggestedFixGenerator().generateLoggingMethod(methodInvocationTree, state, floggerLogContext, migrationContext));
        }

        if (matchLoggingEnabledMethod(methodInvocationTree, state)) {
            return Optional.of(migrateLoggingEnabledMethod(methodName, methodInvocationTree, state, migrationContext));
        }

        return Optional.empty();
    }

    protected final TargetLogLevel mapLogLevel(String level) {
        return targetLogLevelFunction.apply(level);
    }

    protected final FloggerSuggestedFixGenerator getFloggerSuggestedFixGenerator() {
        return floggerSuggestedFixGenerator;
    }

    protected abstract boolean matchImport(Tree qualifiedIdentifier, VisitorState visitorState);
    protected abstract Set<String> loggingPackagePrefixes();

    protected abstract boolean matchLogFactory(VariableTree variableTree, VisitorState visitorState);

    protected abstract boolean matchLoggingEnabledMethod(MethodInvocationTree methodInvocationTree, VisitorState state);

    protected abstract boolean matchLoggingMethod(MethodInvocationTree methodInvocationTree, VisitorState state);

    protected abstract ImmutableFloggerLogContext migrateLoggingMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext);

    protected abstract SuggestedFix migrateLoggingEnabledMethod(String methodName, MethodInvocationTree methodInvocationTree, VisitorState state, MigrationContext migrationContext);

}
