package com.digitalascent.errorprone.flogger.migrate;

import com.digitalascent.errorprone.flogger.migrate.model.FloggerConditionalStatement;
import com.digitalascent.errorprone.flogger.migrate.model.FloggerLogStatement;
import com.digitalascent.errorprone.flogger.migrate.model.ImmutableMigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.MigrationContext;
import com.digitalascent.errorprone.flogger.migrate.model.RefactoringConfiguration;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.Arguments;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggerVariableNamingType;
import com.digitalascent.errorprone.flogger.migrate.sourceapi.LoggingApiSpecification;
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
import com.google.errorprone.util.ASTHelpers;
import com.sun.source.tree.ClassTree;
import com.sun.source.tree.CompilationUnitTree;
import com.sun.source.tree.ImportTree;
import com.sun.source.tree.MethodInvocationTree;
import com.sun.source.tree.Tree;
import com.sun.source.tree.VariableTree;
import com.sun.tools.javac.code.Symbol;

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

    @SuppressWarnings("unused")
    public LoggerApiRefactoringCheck() {
        // EMPTY; required as Error Prone loads via ServiceLoader; actual constructor used will be the one with
        // ErrorProneFlags
        this.refactoringConfiguration = null;
        this.loggingApiSpecification = null;
    }

    public LoggerApiRefactoringCheck(ErrorProneFlags flags) {
        configureLogging();
        logger.atInfo().log("Starting LoggerApiRefactoringCheck with flags: %s", flags.getFlagsMap());
        String sourceApi = flags.get(SOURCE_API_FLAG).orElseThrow(() -> new IllegalArgumentException("Missing source api for option " + SOURCE_API_FLAG));
        this.refactoringConfiguration = new RefactoringConfigurationLoader().loadRefactoringConfiguration("", sourceApi);
        this.loggingApiSpecification = refactoringConfiguration.loggingApiSpecification();
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
                addSuggestedFix(migrateImport(importTree, visitorState));
                return super.visitImport(importTree, visitorState);
            }
        });
    }

    private SuggestedFix migrateImport(ImportTree importTree, VisitorState visitorState) {
        if (loggingApiSpecification.matchImport(importTree.getQualifiedIdentifier(), visitorState)) {
            return refactoringConfiguration.floggerSuggestedFixGenerator().removeImport(importTree);
        }

        if (loggingApiSpecification.loggingPackagePrefixes().stream()
                .anyMatch(x -> importTree.getQualifiedIdentifier().toString().startsWith(x))) {
            return refactoringConfiguration.floggerSuggestedFixGenerator().removeImport(importTree);
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
        suggestedFixes.addAll(migrateLoggingConditional(classTree, state, migrationContext, treeScanner.loggingConditionals()));
        suggestedFixes.addAll(migrateLoggingMethodInvocations(classTree, state, migrationContext, treeScanner.loggingMethodInvocations()));
        suggestedFixes.addAll(migrateLoggingConditionalMethods(classTree, state, migrationContext, treeScanner.loggingEnabledMethods()));


        return suggestedFixes;
    }

    private List<SuggestedFix> migrateLoggingConditionalMethods(ClassTree classTree, VisitorState state,
                                                                MigrationContext migrationContext,
                                                                List<MethodInvocationTree> loggingEnabledMethods) {
        return loggingEnabledMethods.stream().map(loggingEnabledMethod -> {
            try {
                FloggerConditionalStatement floggerConditionalStatement = migrateConditionalMethod(loggingEnabledMethod, state, migrationContext);
                return refactoringConfiguration.floggerSuggestedFixGenerator().generateConditionalMethod(floggerConditionalStatement, state, migrationContext);
            } catch (SkipLogMethodException e) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(), loggingEnabledMethod, e.getMessage());
                return SuggestedFix.builder().build();
            }
        }).collect(toImmutableList());
    }

    private FloggerConditionalStatement migrateConditionalMethod(MethodInvocationTree loggingEnabledMethod, VisitorState state, MigrationContext migrationContext) {
        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(loggingEnabledMethod);
        String methodName = sym.getSimpleName().toString();
        return loggingApiSpecification.parseLoggingConditionalMethod(methodName, loggingEnabledMethod, state, migrationContext);
    }

    private ImmutableList<SuggestedFix> migrateLoggingConditional(ClassTree classTree, VisitorState state, MigrationContext migrationContext,
                                                                  List<LoggingConditional> loggingConditionals) {
        return loggingConditionals.stream().map(loggingConditional -> {
            try {
                return migrateLoggingConditional(loggingConditional, state, migrationContext);
            } catch (SkipLogMethodException e) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(), loggingConditional.loggingConditionalInvocation(), e.getMessage());
                return SuggestedFix.builder().build();
            }
        }).collect(toImmutableList());
    }


    private ImmutableList<SuggestedFix> migrateLoggingMethodInvocations(ClassTree classTree, VisitorState state, MigrationContext migrationContext,
                                                                        List<MethodInvocationTree> loggingMethodInvocations) {
        return loggingMethodInvocations.stream().map(loggingMethodInvocation -> {
            try {
                return migrateLoggingMethodInvocation(loggingMethodInvocation, state, migrationContext);
            } catch (SkipLogMethodException e) {
                logger.atWarning().log("Skipped %s %s: %s", classTree.getSimpleName(), loggingMethodInvocation, e.getMessage());
                return SuggestedFix.builder().build();
            }
        }).collect(toImmutableList());
    }

    private SuggestedFix migrateLoggingMethodInvocation(MethodInvocationTree loggingMethodInvocation, VisitorState state, MigrationContext migrationContext) {
        checkArgument(loggingApiSpecification.matchLoggingMethod(loggingMethodInvocation, state), "matchLoggingMethod(loggingMethodInvocation, state) : %s", loggingMethodInvocation);

        FloggerLogStatement floggerLogStatement = migrateLoggingMethod(loggingMethodInvocation, state, migrationContext);
        return refactoringConfiguration.floggerSuggestedFixGenerator().generateLoggingMethod(loggingMethodInvocation,
                state, floggerLogStatement, migrationContext);
    }

    private FloggerLogStatement migrateLoggingMethod(MethodInvocationTree loggingMethodInvocation, VisitorState state, MigrationContext migrationContext) {
        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(loggingMethodInvocation);
        String methodName = sym.getSimpleName().toString();

        return loggingApiSpecification.parseLoggingMethod(methodName, loggingMethodInvocation, state, migrationContext);
    }

    private SuggestedFix handleLoggerMemberVariables(ClassTree classTree, VisitorState state, MigrationContext migrationContext) throws SkipCompilationUnitException {
        return refactoringConfiguration.floggerSuggestedFixGenerator().processLoggerVariables(classTree, state, migrationContext);
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

    private SuggestedFix migrateLoggingConditional(LoggingConditional loggingConditional,
                                                        VisitorState state, MigrationContext migrationContext) {
        checkArgument(loggingApiSpecification.matchConditionalMethod(loggingConditional.loggingConditionalInvocation(), state),
                "matchLoggingMethod(loggingConditional.loggingConditionalInvocation(), state) : %s",
                loggingConditional);

        Symbol.MethodSymbol sym = ASTHelpers.getSymbol(loggingConditional.loggingConditionalInvocation());
        String methodName = sym.getSimpleName().toString();

        switch (loggingConditional.actionType()) {
            case MIGRATE_EXPRESSION_ONLY: {
                FloggerConditionalStatement conditionalStatement =  loggingApiSpecification.parseLoggingConditionalMethod(methodName, loggingConditional.loggingConditionalInvocation(), state, migrationContext);
                return refactoringConfiguration.floggerSuggestedFixGenerator().generateConditionalMethod(conditionalStatement,state,migrationContext);
            }

            case ELIDE:
                return elideConditional(loggingConditional, state, migrationContext);
        }
        throw new AssertionError("Unknown conditional action type: " + loggingConditional.actionType());
    }

    private SuggestedFix elideConditional(LoggingConditional loggingConditional, VisitorState state,
                                          MigrationContext migrationContext) {
        if (loggingConditional.loggingMethods().isEmpty()) {
            return SuggestedFix.builder().delete(loggingConditional.ifTree()).build();
        }

        List<FloggerLogStatement> logStatements = loggingConditional.loggingMethods().stream()
                .map(loggingMethod -> migrateLoggingMethod(loggingMethod, state, migrationContext))
                .collect(toImmutableList());

        return refactoringConfiguration.floggerSuggestedFixGenerator().elideConditional(loggingConditional.ifTree(), state,
                logStatements, migrationContext);
    }

}
