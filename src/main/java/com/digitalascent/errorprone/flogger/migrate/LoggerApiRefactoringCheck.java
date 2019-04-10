package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.ImmutableMigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.RefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggerVariableNamingType;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.errorprone.BugPattern;
import com.google.errorprone.ErrorProneFlags;
import com.google.errorprone.VisitorState;
import com.google.errorprone.bugpatterns.BugChecker;
import com.google.errorprone.fixes.SuggestedFix;
import com.google.errorprone.matchers.Description;
import com.google.errorprone.matchers.Matcher;
import com.google.errorprone.matchers.Matchers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;

import java.util.ArrayList;
import java.util.List;

import static com.google.common.collect.ImmutableList.toImmutableList;

@AutoService(BugChecker.class)
@BugPattern(
        name = "LoggerApiRefactoring",
        summary = "Refactor logging API",
        severity = BugPattern.SeverityLevel.SUGGESTION,
        tags = BugPattern.StandardTags.REFACTORING)
public final class LoggerApiRefactoringCheck extends BugChecker implements BugChecker.CompilationUnitTreeMatcher {

    private static final String CONFIGURATION_NAMESPACE = "LoggerApiRefactoring";
    private static final String SOURCE_API_FLAG = String.format("%s:%s", CONFIGURATION_NAMESPACE, "SourceApi");

    private final RefactoringConfiguration refactoringConfiguration;
    private final LoggingApiConverter loggingApiConverter;

    public LoggerApiRefactoringCheck() {
        // EMPTY; required as Error Prone loads via ServiceLoader; actual constructor used will be the one with
        // ErrorProneFlags
        this.refactoringConfiguration = null;
        this.loggingApiConverter = null;
    }

    public LoggerApiRefactoringCheck(ErrorProneFlags flags) {
        System.out.println("Starting LoggerApiRefactoringCheck with flags: " + flags.getFlagsMap());
        String sourceApi = flags.get(SOURCE_API_FLAG).orElseThrow(() -> new IllegalArgumentException("Missing source api for option " + SOURCE_API_FLAG));
        this.refactoringConfiguration = new RefactoringConfigurationLoader().loadRefactoringConfiguration("", sourceApi);
        this.loggingApiConverter = refactoringConfiguration.loggingApiConverter();
    }

    @Override
    public Description matchCompilationUnit(CompilationUnitTree compilationUnitTree, VisitorState state) {
        List<SuggestedFix> suggestedFixes = compilationUnitTree.getTypeDecls().stream()
                .filter(ClassTree.class::isInstance)
                .map(o -> processClassTree((ClassTree) o, state))
                .flatMap(List::stream)
                .collect(toImmutableList());

        if (suggestedFixes.isEmpty()) {
            return Description.NO_MATCH;
        }

        List<SuggestedFix> fixes = new ArrayList<>(suggestedFixes);
        fixes.addAll(handleImports(compilationUnitTree, state));

        return describeMatch(compilationUnitTree, SuggestedFixes.merge(fixes));
    }

    private List<SuggestedFix> processClassTree(ClassTree classTree, VisitorState state) {
        try {
            MigrationContext migrationContext = createMigrationContext(classTree, state);
            List<SuggestedFix> fixes = new ArrayList<>(handleMethodInvocations(classTree, state, migrationContext));

            if (!fixes.isEmpty()) {
                // only process / add logger member variables if we've converted logging methods
                fixes.add(handleLoggerMemberVariables(classTree, state, migrationContext));
            }

            return fixes;
        } catch (SkipCompilationUnitException e) {
            System.out.printf("Skipped %s: %s\n", classTree.getSimpleName(), e.getMessage());
            return ImmutableList.of();
        }
    }

    private List<SuggestedFix> handleImports(CompilationUnitTree compilationUnitTree, VisitorState state) {
        return scanTree(compilationUnitTree, state, new FixCollectingTreeScanner() {
            @Override
            public Void visitImport(ImportTree importTree, VisitorState visitorState) {
                addSuggestedFix(loggingApiConverter.migrateImport(importTree, visitorState));
                return super.visitImport(importTree, visitorState);
            }
        });
    }

    private List<SuggestedFix> scanTree(Tree tree, VisitorState visitorState, FixCollectingTreeScanner treeScanner) {
        tree.accept(treeScanner, visitorState);
        return treeScanner.suggestedFixes();
    }

    private List<SuggestedFix> handleMethodInvocations(ClassTree classTree, VisitorState state, MigrationContext migrationContext) {
        return scanTree(classTree, state, new FixCollectingTreeScanner() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree methodInvocationTree, VisitorState visitorState) {
                try {
                    addSuggestedFix(loggingApiConverter.migrateLoggingMethodInvocation(methodInvocationTree, state, migrationContext));
                } catch (SkipLogMethodException e) {
                    System.out.printf("Skipped %s %s: %s\n", classTree.getSimpleName(), methodInvocationTree.toString(), e.getMessage());
                }
                return super.visitMethodInvocation(methodInvocationTree, visitorState);
            }
        });
    }

    private SuggestedFix handleLoggerMemberVariables(ClassTree classTree, VisitorState state, MigrationContext migrationContext) throws SkipCompilationUnitException {
        return refactoringConfiguration.floggerSuggestedFixGenerator().processLoggerVariables(classTree,state,migrationContext);
    }

    private List<VariableTree> findClassNamedLoggers(ClassTree classTree, VisitorState state) {
        MemberVariableScanner scanner = new MemberVariableScanner(
                tree -> loggingApiConverter.determineLoggerVariableNamingType(classTree, tree, state) == LoggerVariableNamingType.CLASS_NAMED);
        classTree.accept(scanner, state);
        return scanner.loggerVariables();
    }

    private List<VariableTree> findNonClassNamedLoggers(ClassTree classTree, VisitorState state) {
        MemberVariableScanner scanner = new MemberVariableScanner(
                tree -> loggingApiConverter.determineLoggerVariableNamingType(classTree, tree, state) == LoggerVariableNamingType.NON_CLASS_NAMED);
        classTree.accept(scanner, state);
        return scanner.loggerVariables();
    }

    private List<VariableTree> findFloggerMemberVariables(Tree typeDecl, VisitorState state) {
        Matcher<Tree> matcher = Matchers.isSubtypeOf(refactoringConfiguration.loggerVariableDefinition().typeQualified());

        MemberVariableScanner scanner = new MemberVariableScanner(tree -> matcher.matches(tree, state));
        typeDecl.accept(scanner, state);
        return scanner.loggerVariables();
    }

    private MigrationContext createMigrationContext(ClassTree classTree, VisitorState visitorState) {
        ImmutableMigrationContext.Builder builder = ImmutableMigrationContext.builder();
        builder.addAllClassNamedLoggers(findClassNamedLoggers(classTree, visitorState));
        builder.addAllNonClassNamedLoggers(findNonClassNamedLoggers(classTree,visitorState));
        builder.addAllFloggerLoggers(findFloggerMemberVariables(classTree, visitorState));

        return builder.build();
    }
}
