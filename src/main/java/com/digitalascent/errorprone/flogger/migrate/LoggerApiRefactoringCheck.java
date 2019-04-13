package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.ImmutableMigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.RefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggerVariableNamingType;
import com.google.auto.service.AutoService;
import com.google.common.collect.ImmutableList;
import com.google.common.flogger.FluentLogger;
import com.google.common.io.Resources;
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
import com.sun.source.util.TreeScanner;
import com.sun.tools.javac.tree.JCTree;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import static com.google.common.collect.ImmutableList.toImmutableList;

@AutoService(BugChecker.class)
@BugPattern(
        name = "LoggerApiRefactoring",
        summary = "Refactor logging API",
        severity = BugPattern.SeverityLevel.SUGGESTION,
        tags = BugPattern.StandardTags.REFACTORING)
public final class LoggerApiRefactoringCheck extends BugChecker implements BugChecker.CompilationUnitTreeMatcher {
    private final FluentLogger logger = FluentLogger.forEnclosingClass();

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
        configureLogging();
        logger.atInfo().log("Starting LoggerApiRefactoringCheck with flags: %s", flags.getFlagsMap());
        String sourceApi = flags.get(SOURCE_API_FLAG).orElseThrow(() -> new IllegalArgumentException("Missing source api for option " + SOURCE_API_FLAG));
        this.refactoringConfiguration = new RefactoringConfigurationLoader().loadRefactoringConfiguration("", sourceApi);
        this.loggingApiConverter = refactoringConfiguration.loggingApiConverter();
    }

    private void configureLogging() {
        URL url = Resources.getResource(getClass(), "logging.properties");
        try (InputStream inputStream = Resources.asByteSource(url).openStream()) {
            LogManager.getLogManager().readConfiguration(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public Description matchCompilationUnit(CompilationUnitTree compilationUnitTree, VisitorState state) {
        List<SuggestedFix> suggestedFixes = compilationUnitTree.getTypeDecls().stream()
                .filter(ClassTree.class::isInstance)
                .map(o -> processClassTree((ClassTree) o, state))
                .flatMap(List::stream)
                .filter(fix -> !fix.isEmpty())
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
            logger.atInfo().log("Processing %s", classTree.getSimpleName());
            MigrationContext migrationContext = createMigrationContext(classTree, state);
            logger.atFine().log("Migration context: %s", migrationContext);

            List<SuggestedFix> fixes = new ArrayList<>(handleMethodInvocations(classTree, state, migrationContext));

            if (!fixes.stream().allMatch(SuggestedFix::isEmpty)) {
                // only process / add logger member variables if we've converted logging methods
                fixes.add(handleLoggerMemberVariables(classTree, state, migrationContext));
            }
            logger.atInfo().log("Processed %s", classTree.getSimpleName());
            return fixes;
        } catch (SkipCompilationUnitException e) {
            logger.atWarning().log("Skipped %s: %s", classTree.getSimpleName(), e.getMessage());
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
        List<MethodInvocationTree> loggingMethodInvocations = new ArrayList<>();
        List<MethodInvocationTree> loggingEnabledMethodInvocations = new ArrayList<>();

        classTree.accept(new TreeScanner<Void, VisitorState>() {
            @Override
            public Void visitMethodInvocation(MethodInvocationTree node, VisitorState visitorState) {
                if (loggingApiConverter.matchLoggingEnabledMethod(node, visitorState) && loggingApiConverter.matchLoggingMethod(node, visitorState)) {
                    throw new IllegalStateException("Cannot be a logging method and a logging enabled method: " + node);
                }

                String variableName = null;
                Tree methodSelect = node.getMethodSelect();
                if (methodSelect instanceof JCTree.JCFieldAccess) {
                    variableName = ((JCTree.JCFieldAccess) methodSelect).selected.toString();
                }

                if (!isIgnoredLogger(variableName, migrationContext)) {
                    if (loggingApiConverter.matchLoggingMethod(node, visitorState)) {
                        loggingMethodInvocations.add(node);
                    }
                    if (loggingApiConverter.matchLoggingEnabledMethod(node, visitorState)) {
                        loggingEnabledMethodInvocations.add(node);
                    }
                }
                return super.visitMethodInvocation(node, visitorState);
            }
        }, state);

        final List<SuggestedFix> suggestedFixes = new ArrayList<>();
        for (MethodInvocationTree loggingMethodInvocation : loggingMethodInvocations) {
            try {
                suggestedFixes.add(loggingApiConverter.migrateLoggingMethodInvocation(loggingMethodInvocation, state, migrationContext));
            } catch (SkipLogMethodException e) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(), loggingMethodInvocation, e.getMessage());
            }
        }

        for (MethodInvocationTree loggingEnabledMethodInvocation : loggingEnabledMethodInvocations) {
            try {
                suggestedFixes.add(loggingApiConverter.migrateLoggingEnabledMethodInvocation(loggingEnabledMethodInvocation, state, migrationContext));
            } catch( SkipLogMethodException e ) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(), loggingEnabledMethodInvocation, e.getMessage());
            }
        }
        return suggestedFixes;
    }

    private boolean isIgnoredLogger(@Nullable String variableName, MigrationContext migrationContext) {
        return migrationContext.nonClassNamedLoggers().stream()
                .anyMatch(loggerVariable -> loggerVariable.getName().toString().equals(variableName));
    }

    private SuggestedFix handleLoggerMemberVariables(ClassTree classTree, VisitorState state, MigrationContext migrationContext) throws SkipCompilationUnitException {
        return refactoringConfiguration.floggerSuggestedFixGenerator().processLoggerVariables(classTree, state, migrationContext);
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
        builder.addAllNonClassNamedLoggers(findNonClassNamedLoggers(classTree, visitorState));
        builder.addAllFloggerLoggers(findFloggerMemberVariables(classTree, visitorState));

        return builder.build();
    }
}
