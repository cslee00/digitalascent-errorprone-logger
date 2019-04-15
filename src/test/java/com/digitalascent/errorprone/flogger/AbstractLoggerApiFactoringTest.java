package com.digitalascent.errorprone.flogger;

import com.digitalascent.errorprone.flogger.migrate.LoggerApiRefactoringCheck;
import com.google.common.base.CharMatcher;
import com.google.common.base.Splitter;
import com.google.errorprone.BugCheckerRefactoringTestHelper;
import com.google.errorprone.ErrorProneFlags;

abstract class AbstractLoggerApiFactoringTest {

    protected void executeTest(String sourceApiName, String testCode, String expectedCode) {
        ErrorProneFlags flags = ErrorProneFlags.builder()
                .putFlag("LoggerApiRefactoring:SourceApi", sourceApiName)
                .build();

        String[] testLines = Splitter.on(CharMatcher.anyOf("\r\n")).splitToList(testCode).toArray(new String[0]);
        String[] expectedLines = Splitter.on(CharMatcher.anyOf("\r\n")).splitToList(expectedCode).toArray(new String[0]);

        BugCheckerRefactoringTestHelper.newInstance(new LoggerApiRefactoringCheck(flags), getClass())
                .addInputLines("TestClass.java", testLines)
                .addOutputLines("TestClass.java", expectedLines)
                .allowBreakingChanges()
                .doTest(BugCheckerRefactoringTestHelper.TestMode.TEXT_MATCH);
    }
}
