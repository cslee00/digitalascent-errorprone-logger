package com.digitalascent.errorprone.flogger;

import com.digitalascent.errorprone.flogger.migrate.LoggerApiRefactoringCheck;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.ErrorProneFlags;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoggerApiRefactoringCheckTest {

    @ParameterizedTest(name = "multiple-loggers: test id {arguments}")
    @ValueSource(ints = {0, 1, 2})
    public void testMultipleLoggers(int id) {
        executeTest("slf4j", "MultipleLoggers", id);
    }

    private void executeTest(String sourceApiName, String loggerName, int index) {
        ErrorProneFlags flags = ErrorProneFlags.builder()
                .putFlag("LoggerApiRefactoring:SourceApi", sourceApiName)
                .build();
        String prefix = "testdata/TestClassUsing";
        String input = String.format("%s%s_%d.java", prefix, loggerName, index);
        String expected = String.format("%s%s_%d_expected.java", prefix, loggerName, index);

        BugCheckerRefactoringTestHelper.newInstance(new LoggerApiRefactoringCheck(flags), getClass())
                .addInput(input)
                .addOutput(expected)
                .allowBreakingChanges()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.AST_MATCH);
    }
}