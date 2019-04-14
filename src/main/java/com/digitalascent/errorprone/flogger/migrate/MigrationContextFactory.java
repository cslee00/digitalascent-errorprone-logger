package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.ImmutableMigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.LoggerVariableDefinition;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.source.Arguments;
import com.digitalascent.errorprone.flogger.migrate.source.LoggerVariableNamingType;
import com.digitalascent.errorprone.flogger.migrate.source.api.LoggingApiSpecification;
import com.google.errorprone.VisitorState;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.List;

import static java.util.Objects.requireNonNull;

final class MigrationContextFactory {
    private final LoggingApiSpecification loggingApiSpecification;
    private final LoggerVariableDefinition loggerVariableDefinition;

    MigrationContextFactory(LoggingApiSpecification loggingApiSpecification, LoggerVariableDefinition loggerVariableDefinition) {
        this.loggingApiSpecification = requireNonNull(loggingApiSpecification, "loggingApiSpecification");
        this.loggerVariableDefinition = requireNonNull(loggerVariableDefinition, "loggerVariableDefinition");
    }

    MigrationContext createMigrationContext(ClassTree classTree, VisitorState visitorState) {
        ImmutableMigrationContext.Builder builder = ImmutableMigrationContext.builder();
        builder.addAllClassNamedLoggers(findClassNamedLoggers(classTree, visitorState));
        builder.addAllNonClassNamedLoggers(findNonClassNamedLoggers(classTree, visitorState));
        builder.addAllFloggerLoggers(findFloggerMemberVariables(classTree, visitorState));
        builder.className(classTree.getSimpleName().toString());

        return builder.build();
    }

    private List<VariableTree> findClassNamedLoggers(ClassTree classTree, VisitorState state) {
        MemberVariableScanner scanner = new MemberVariableScanner(
                tree -> determineLoggerVariableNamingType(classTree, tree, state) == LoggerVariableNamingType.CLASS_NAMED);
        classTree.accept(scanner, state);
        return scanner.loggerVariables();
    }

    private List<VariableTree> findNonClassNamedLoggers(ClassTree classTree, VisitorState state) {
        MemberVariableScanner scanner = new MemberVariableScanner(
                tree -> determineLoggerVariableNamingType(classTree, tree, state) == LoggerVariableNamingType.NON_CLASS_NAMED);
        classTree.accept(scanner, state);
        return scanner.loggerVariables();
    }

    private LoggerVariableNamingType determineLoggerVariableNamingType(ClassTree classTree, VariableTree variableTree, VisitorState visitorState) {
        if (!loggingApiSpecification.matchLogFactory(variableTree, visitorState)) {
            return LoggerVariableNamingType.NOT_LOGGER;
        }

        MethodInvocationTree logManagerMethodInvocationTree = (MethodInvocationTree) variableTree.getInitializer();
        if (logManagerMethodInvocationTree.getArguments().isEmpty() || Arguments.isLoggerNamedAfterClass(classTree,
                logManagerMethodInvocationTree.getArguments().get(0), visitorState)) {
            return LoggerVariableNamingType.CLASS_NAMED;
        }

        return LoggerVariableNamingType.NON_CLASS_NAMED;
    }

    private List<VariableTree> findFloggerMemberVariables(Tree typeDecl, VisitorState state) {
        Matcher<Tree> matcher = Matchers.isSubtypeOf(loggerVariableDefinition.typeQualified());

        MemberVariableScanner scanner = new MemberVariableScanner(tree -> matcher.matches(tree, state));
        typeDecl.accept(scanner, state);
        return scanner.loggerVariables();
    }

}
