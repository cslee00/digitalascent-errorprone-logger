package com.digitalascent.errorprone.flogger;

import com.digitalascent.errorprone.flogger.migrate.LoggerApiRefactoring;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.ErrorProneFlags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoggerApiRefactoringTest {

    @Test
    public void testSlf4j() {
        executeTest("slf4j", "Slf4j", 0 );
    }

    @Test
    public void testLog4j() {
        executeTest("log4j", "Log4j", 0);
    }

    @ParameterizedTest( name = "common-logging: test id {arguments}")
    @ValueSource(ints = {0,1,2,3,4})
    public void testCommonsLogging( int id ) {
        executeTest("commons-logging", "CommonsLogging", id);
    }

    @Test
    public void testTinyLog() {
        executeTest("tinylog", "TinyLog", 0);
    }

    @Test
    public void testTinyLog2() {
        executeTest("tinylog2", "TinyLog2", 0);
    }

    @Test
    public void testJUL() {
        executeTest("jul", "JUL", 0);
    }

    @Test
    public void testLog4j2() {
        executeTest("log4j2", "Log4j2", 0);
    }

    private void executeTest(String sourceApiName, String loggerName, int index) {
        ErrorProneFlags flags = ErrorProneFlags.builder()
                .putFlag("LoggerApiRefactoring:SourceApi", sourceApiName)
                .putFlag("LoggerApiRefactoring:Debug", "true")
                .build();
        String prefix = "testdata/TestClassUsing";
        String input = String.format("%s%s_%d.java", prefix, loggerName, index);
        String expected = String.format("%s%s_%d_expected.java", prefix, loggerName, index);

        BugCheckerRefactoringTestHelper.newInstance(new LoggerApiRefactoring(flags), getClass())
                .addInput(input)
                .addOutput(expected)
                .allowBreakingChanges()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.AST_MATCH);
    }
}