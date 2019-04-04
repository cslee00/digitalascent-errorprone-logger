package com.digitalascent.errorprone.flogger.migrate;

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

@AutoService(LoggerApiRefactoring.class)
@BugPattern(
        name = "LoggerApiRefactoring",
        summary = "Refactor logging API",
        severity = BugPattern.SeverityLevel.SUGGESTION,
        tags = BugPattern.StandardTags.REFACTORING)
public final class LoggerApiRefactoring extends BugChecker implements BugChecker.CompilationUnitTreeMatcher {

    private static final String CONFIGURATION_NAMESPACE = "LoggerApiRefactoring";
    private static final String SOURCE_API_FLAG = String.format("%s:%s",CONFIGURATION_NAMESPACE, "SourceApi" );

    private final RefactoringConfiguration refactoringConfiguration;
    private final LoggingApiConverter loggingApiConverter;
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator = new FloggerSuggestedFixGenerator();

    public LoggerApiRefactoring(ErrorProneFlags flags) {
        System.out.println("Starting LoggerApiRefactoring with flags: " + flags);
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
                fixes.addAll(handleLoggerMemberVariables(classTree, state, migrationContext));
            }

            return fixes;
        } catch (SkipCompilationUnitException e) {
            // TODO - print diag message
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
                } catch( SkipLogMethodException e ) {
                    // TODO - diag message
                }
                return super.visitMethodInvocation(methodInvocationTree, visitorState);
            }
        });
    }

    private List<SuggestedFix> handleLoggerMemberVariables(ClassTree classTree, VisitorState state, MigrationContext migrationContext) throws SkipCompilationUnitException {
        List<SuggestedFix> suggestedFixes = new ArrayList<>();
        List<VariableTree> loggerVariables = migrationContext.sourceLoggerMemberVariables();

        switch (loggerVariables.size()) {
            case 0:
                suggestedFixes.add(floggerSuggestedFixGenerator.generateLoggerVariable(classTree, null, state, migrationContext));
                break;
            case 1:
                loggingApiConverter.migrateLoggerVariable(classTree, loggerVariables.get(0), state, migrationContext).ifPresent(suggestedFixes::add);
                break;

            default:
                // we can't handle more than one logger per-class at present, in part due to Flogger being tied to class name
                throw new SkipCompilationUnitException("Unable to migrate logger class with multiple loggers");
        }

        return ImmutableList.copyOf(suggestedFixes);
    }

    private List<VariableTree> findSourceLoggerMemberVariables(Tree typeDecl, VisitorState state) {
        LoggerMemberVariableScanner scanner = new LoggerMemberVariableScanner(tree -> loggingApiConverter.isLoggerVariable(tree, state));
        typeDecl.accept(scanner, state);
        return scanner.loggerVariables();
    }

    private List<VariableTree> findFloggerMemberVariables(Tree typeDecl, VisitorState state) {
        // TODO - configurable
        Matcher<Tree> matcher = Matchers.isSubtypeOf("com.google.common.flogger.FluentLogger");

        LoggerMemberVariableScanner scanner = new LoggerMemberVariableScanner(tree -> matcher.matches(tree,state));
        typeDecl.accept(scanner, state);
        return scanner.loggerVariables();
    }

    private MigrationContext createMigrationContext(ClassTree classTree, VisitorState visitorState) {
        ImmutableMigrationContext.Builder builder = ImmutableMigrationContext.builder();
        List<VariableTree> sourceLoggerMemberVariables = findSourceLoggerMemberVariables(classTree, visitorState);
        builder.addAllSourceLoggerMemberVariables(sourceLoggerMemberVariables);
        if( sourceLoggerMemberVariables.size() == 1 ) {
            builder.sourceLoggerMemberVariableName( sourceLoggerMemberVariables.get(0).getName().toString() );
        }

        List<VariableTree> floggerMemberVariables = findFloggerMemberVariables(classTree, visitorState);
        builder.addAllFloggerMemberVariables(floggerMemberVariables);
        if( floggerMemberVariables.size() == 1 ) {
            builder.floggerMemberVariableName( floggerMemberVariables.get(0).getName().toString() );
        }

        return builder.build();
    }
}
