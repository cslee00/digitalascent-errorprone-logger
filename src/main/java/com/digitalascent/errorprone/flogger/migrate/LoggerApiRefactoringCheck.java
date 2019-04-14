package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.RefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggingApiSpecification;
import com.digitalascent.errorprone.flogger.migrate.target.FloggerSuggestedFixGenerator;
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
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.Tree;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.LogManager;

import static com.google.common.base.Preconditions.checkArgument;
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
    private final LoggingApiSpecification loggingApiSpecification;
    private final FloggerSuggestedFixGenerator floggerSuggestedFixGenerator;

    @SuppressWarnings("unused")
    public LoggerApiRefactoringCheck() {
        // EMPTY; required as Error Prone loads via ServiceLoader; actual constructor used will be the one with
        // ErrorProneFlags
        this.refactoringConfiguration = null;
        this.loggingApiSpecification = null;
        this.floggerSuggestedFixGenerator = null;
    }

    public LoggerApiRefactoringCheck(ErrorProneFlags flags) {
        configureLogging();
        logger.atInfo().log("Starting LoggerApiRefactoringCheck with flags: %s", flags.getFlagsMap());
        String sourceApi = flags.get(SOURCE_API_FLAG).orElseThrow(() -> new IllegalArgumentException("Missing source api for option " + SOURCE_API_FLAG));
        this.refactoringConfiguration = new RefactoringConfigurationLoader().loadRefactoringConfiguration("", sourceApi);
        this.loggingApiSpecification = refactoringConfiguration.loggingApiSpecification();
        this.floggerSuggestedFixGenerator = refactoringConfiguration.floggerSuggestedFixGenerator();
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
            MigrationContext migrationContext = new MigrationContextFactory(loggingApiSpecification, refactoringConfiguration.loggerVariableDefinition())
                    .createMigrationContext(classTree, state);
            logger.atFine().log("Migration context: %s", migrationContext);

            List<SuggestedFix> fixes = new ArrayList<>(handleMethodInvocations(classTree, state, migrationContext));

            if (!fixes.stream().allMatch(SuggestedFix::isEmpty)) {
                // only process / add logger member variables if we've converted logging methods
                fixes.add(floggerSuggestedFixGenerator.processLoggerVariables(classTree, state, migrationContext));
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
                addSuggestedFix(migrateImport(importTree, visitorState));
                return super.visitImport(importTree, visitorState);
            }
        });
    }

    private SuggestedFix migrateImport(ImportTree importTree, VisitorState visitorState) {
        if (loggingApiSpecification.matchImport(importTree.getQualifiedIdentifier(), visitorState)) {
            return floggerSuggestedFixGenerator.removeImport(importTree);
        }

        if (loggingApiSpecification.loggingPackagePrefixes().stream()
                .anyMatch(x -> importTree.getQualifiedIdentifier().toString().startsWith(x))) {
            return floggerSuggestedFixGenerator.removeImport(importTree);
        }
        return SuggestedFix.builder().build();
    }

    private List<SuggestedFix> scanTree(Tree tree, VisitorState visitorState, FixCollectingTreeScanner treeScanner) {
        tree.accept(treeScanner, visitorState);
        return treeScanner.suggestedFixes();
    }

    private List<SuggestedFix> handleMethodInvocations(ClassTree classTree, VisitorState state, MigrationContext migrationContext) {

        LoggerInvocationTreeScanner treeScanner = new LoggerInvocationTreeScanner(migrationContext, loggingApiSpecification);
        //noinspection ResultOfMethodCallIgnored
        treeScanner.scan(classTree, state);

        final List<SuggestedFix> suggestedFixes = new ArrayList<>();
        suggestedFixes.addAll(migrateLoggingConditionals(classTree, migrationContext, treeScanner.loggingConditionals()));
        suggestedFixes.addAll(migrateLoggingMethodInvocations(classTree, migrationContext, treeScanner.loggingMethodInvocations()));
        suggestedFixes.addAll(migrateLoggingConditionalMethods(classTree, migrationContext, treeScanner.loggingEnabledMethods()));

        return suggestedFixes;
    }

    private List<SuggestedFix> migrateLoggingConditionalMethods(ClassTree classTree,
                                                                MigrationContext migrationContext,
                                                                List<MethodInvocation> loggingEnabledMethods) {
        return loggingEnabledMethods.stream().map(loggingEnabledMethod -> {
            try {
                FloggerConditionalStatement floggerConditionalStatement = migrateConditionalMethod(loggingEnabledMethod, migrationContext);
                return floggerSuggestedFixGenerator.generateConditionalMethod(floggerConditionalStatement, migrationContext);
            } catch (SkipLogMethodException e) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(), loggingEnabledMethod, e.getMessage());
                return SuggestedFix.builder().build();
            }
        }).collect(toImmutableList());
    }

    private FloggerConditionalStatement migrateConditionalMethod(MethodInvocation loggingEnabledMethod, MigrationContext migrationContext) {
        return loggingApiSpecification.parseLoggingConditionalMethod(loggingEnabledMethod);
    }

    private ImmutableList<SuggestedFix> migrateLoggingConditionals(ClassTree classTree, MigrationContext migrationContext,
                                                                   List<LoggingConditional> loggingConditionals) {
        return loggingConditionals.stream().map(loggingConditional -> {
            try {
                return migrateLoggingConditionals(loggingConditional, migrationContext);
            } catch (SkipLogMethodException e) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(),
                        loggingConditional.loggingConditionalInvocation(), e.getMessage());
                return SuggestedFix.builder().build();
            }
        }).collect(toImmutableList());
    }

    private ImmutableList<SuggestedFix> migrateLoggingMethodInvocations(ClassTree classTree, MigrationContext migrationContext,
                                                                        List<MethodInvocation> loggingMethodInvocations) {
        return loggingMethodInvocations.stream().map(loggingMethodInvocation -> {
            try {
                return migrateLoggingMethodInvocation(loggingMethodInvocation, migrationContext);
            } catch (SkipLogMethodException e) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(), loggingMethodInvocation, e.getMessage());
                return SuggestedFix.builder().build();
            }
        }).collect(toImmutableList());
    }

    private SuggestedFix migrateLoggingMethodInvocation(MethodInvocation loggingMethodInvocation, MigrationContext migrationContext) {
        checkArgument(loggingApiSpecification.matchLoggingMethod(loggingMethodInvocation.tree(), loggingMethodInvocation.state()),
                "matchLoggingMethod(loggingMethodInvocation, state) : %s", loggingMethodInvocation);

        FloggerLogStatement floggerLogStatement = loggingApiSpecification.parseLoggingMethod(loggingMethodInvocation, migrationContext);
        return floggerSuggestedFixGenerator.generateLoggingMethod(loggingMethodInvocation,
                floggerLogStatement, migrationContext);
    }

    private SuggestedFix migrateLoggingConditionals(LoggingConditional loggingConditional,
                                                    MigrationContext migrationContext) {
        checkArgument(loggingApiSpecification.matchConditionalMethod(loggingConditional.loggingConditionalInvocation().tree(), loggingConditional.loggingConditionalInvocation().state()),
                "matchLoggingMethod(loggingConditional.loggingConditionalInvocation(), state) : %s",
                loggingConditional);

        switch (loggingConditional.actionType()) {
            case MIGRATE_EXPRESSION_ONLY: {
                FloggerConditionalStatement conditionalStatement = loggingApiSpecification.parseLoggingConditionalMethod(loggingConditional.loggingConditionalInvocation());
                return floggerSuggestedFixGenerator.generateConditionalMethod(conditionalStatement, migrationContext);
            }

            case ELIDE:
                return elideConditional(loggingConditional, migrationContext);
        }
        throw new AssertionError("Unknown conditional action type: " + loggingConditional.actionType());
    }

    private SuggestedFix elideConditional(LoggingConditional loggingConditional,
                                          MigrationContext migrationContext) {
        if (loggingConditional.loggingMethods().isEmpty()) {
            return SuggestedFix.builder().delete(loggingConditional.ifTree()).build();
        }

        List<FloggerLogStatement> logStatements = loggingConditional.loggingMethods().stream()
                .map(loggingMethod -> loggingApiSpecification.parseLoggingMethod(loggingMethod, migrationContext))
                .collect(toImmutableList());

        return floggerSuggestedFixGenerator.elideConditional(loggingConditional.ifTree(),
                logStatements, migrationContext, loggingConditional.loggingConditionalInvocation().state() );
    }
}
