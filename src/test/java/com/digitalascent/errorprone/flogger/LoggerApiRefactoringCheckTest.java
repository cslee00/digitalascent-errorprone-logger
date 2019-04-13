package com.digitalascent.errorprone.flogger;

import com.digitalascent.errorprone.flogger.migrate.LoggerApiRefactoringCheck;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.ErrorProneFlags;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LoggerApiRefactoringCheckTest {

    @ParameterizedTest( name = "slf4j: test id {arguments}")
    @ValueSource(ints = {0,1,2,3,4,5,6,7,8})
    public void testSlf4j(int id) {
        executeTest("slf4j", "Slf4j", id );
    }

    @ParameterizedTest( name = "log4j: test id {arguments}")
    @ValueSource(ints = {0,1,2,3,4,5,6,7})
    public void testLog4j(int id) {
        executeTest("log4j", "Log4j", id);
    }

    @ParameterizedTest( name = "log4j2: test id {arguments}")
    @ValueSource(ints = {0,1,2,3,4,5,6})
    public void testLog4j2(int id) {
        executeTest("log4j2", "Log4j2", id);
    }

    @ParameterizedTest( name = "common-logging: test id {arguments}")
    @ValueSource(ints = {0,1,2,3,4,5,6})
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
    public void testMessageFormatArguments() {
        executeTest("slf4j", "MessageFormatArguments", 0);
    }

    @ParameterizedTest( name = "multiple-loggers: test id {arguments}")
    @ValueSource(ints = {0,1, 2})
    public void testMultipleLoggers(int id) {
        executeTest("slf4j", "MultipleLoggers", id);
    }

    @ParameterizedTest( name = "conditionals: test id {arguments}")
    @ValueSource(ints = {0,1})
    public void testConditionals(int id) {
        executeTest("slf4j", "Conditionals", id);
    }

    @ParameterizedTest( name = "jul: test id {arguments}")
    @ValueSource(ints = {0,1,2,3,4})
    public void testJUL(int id) {
        executeTest("jul", "JUL", id);
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