package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog2;


class TinyLog2MessageFormatterTest {

 /*   @Test
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
        String expected = capturingWriter.getMessage().replace("\r\n","");

        String actual = argsToReplace > 0 ? TinyLog2MessageFormatter.format(formatString) : formatString;

        softAssertions.assertThat(actual).as("Format string: '" + formatString + "'").isEqualTo(expected);
    }

    private static final class CapturingWriter implements Writer {
        private final StringWriter stringWriter = new StringWriter();

        public String getMessage() {
            return stringWriter.toString();
        }

        @Override
        public Set<LogEntryValue> getRequiredLogEntryValues() {
            return new HashSet<>( ImmutableSet.of(LogEntryValue.MESSAGE ) );
        }

        @Override
        public void write(LogEntry logEntry) throws Exception {
            stringWriter.write(logEntry.getMessage());
        }

        @Override
        public void flush() throws Exception {
            stringWriter.flush();
        }

        @Override
        public void close() throws Exception {
            stringWriter.close();
        }
    }*/
}