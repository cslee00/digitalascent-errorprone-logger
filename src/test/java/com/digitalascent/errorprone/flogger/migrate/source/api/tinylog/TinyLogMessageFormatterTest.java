package com.digitalascent.errorprone.flogger.migrate.source.api.tinylog;


import com.google.common.base.CharMatcher;
import com.google.common.collect.ImmutableSet;
import org.assertj.core.api.SoftAssertions;
import org.junit.jupiter.api.Test;
import org.pmw.tinylog.Configuration;
import org.pmw.tinylog.Configurator;
import org.pmw.tinylog.Level;
import org.pmw.tinylog.LogEntry;
import org.pmw.tinylog.Logger;
import org.pmw.tinylog.writers.LogEntryValue;
import org.pmw.tinylog.writers.Writer;

import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

class TinyLogMessageFormatterTest {

    @Test
    void test() {
        SoftAssertions softly = new SoftAssertions();
        format(softly, "{}", 1);
        format(softly, "{} {} {}", 3);
        format(softly, "abc {} {} {} def", 3);
        format(softly, "\\{}", 0);
        format(softly, "\\\\{}", 1);
        format(softly, "5% of {}", 1);
        softly.assertAll();
    }

    private void format(SoftAssertions softAssertions, String formatString, int argsToReplace) {

        Object[] args = new Object[argsToReplace];
        Arrays.fill(args, "%s");

        CapturingWriter capturingWriter = new CapturingWriter();
        Configurator.defaultConfig()
                .removeAllWriters()
                .writer(capturingWriter)
                .formatPattern("{message}")
                .level(Level.DEBUG)
                .activate();
        Logger.info(formatString.replace("%", "%%"), args);
        CharMatcher newLineMatcher = CharMatcher.anyOf("\r\n");
        String expected = newLineMatcher.removeFrom(capturingWriter.getMessage());

        String actual = argsToReplace > 0 ? TinyLogMessageFormatter.format(formatString) : formatString;

        softAssertions.assertThat(actual).as("Format string: '" + formatString + "'").isEqualTo(expected);
    }

    private static final class CapturingWriter implements Writer {
        private final StringWriter stringWriter = new StringWriter();

        public String getMessage() {
            return stringWriter.toString();
        }

        @Override
        public Set<LogEntryValue> getRequiredLogEntryValues() {
            return new HashSet<>( ImmutableSet.of(LogEntryValue.MESSAGE, LogEntryValue.RENDERED_LOG_ENTRY) );
        }

        @Override
        public void init(Configuration configuration) throws Exception {

        }

        @Override
        public void write(LogEntry logEntry) throws Exception {
            stringWriter.write(logEntry.getRenderedLogEntry());
        }

        @Override
        public void flush() throws Exception {
            stringWriter.flush();
        }

        @Override
        public void close() throws Exception {
            stringWriter.close();
        }
    }
}