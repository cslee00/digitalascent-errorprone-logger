package com.digitalascent.errorprone.flogger.migrate.sourceapi.slf4j;

/**
 * Converts SLF4J parameter placeholder '{}' into printf-style parameter '%s'.
 * Based on SLF4J org.slf4j.helpers.MessageFormatter to ensure accuracy of parsing
 */
final class Slf4jMessageFormatConverter {

    private static final String DELIM_STR = "{}";
    private static final char ESCAPE_CHAR = '\\';

    static String convertMessageFormat(String messagePattern ) {

        // escape '%' sign in incoming format
        messagePattern = messagePattern.replace("%", "%%");

        int nextIndex = 0;
        int delimiterIndex;
        StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);

        while( true ) {
            delimiterIndex = messagePattern.indexOf(DELIM_STR, nextIndex);

            if (delimiterIndex == -1) {
                if (nextIndex == 0) {
                    return messagePattern;
                } else {
                    sbuf.append(messagePattern, nextIndex, messagePattern.length());
                    return sbuf.toString();
                }
            } else {
                if (isEscapedDelimeter(messagePattern, delimiterIndex)) {
                    if (isDoubleEscaped(messagePattern, delimiterIndex)) {
                        sbuf.append(messagePattern, nextIndex, delimiterIndex - 1 );
                        appendPrintfPlaceholder(sbuf);
                        nextIndex = delimiterIndex + 2;
                    } else {
                        sbuf.append(messagePattern, nextIndex, delimiterIndex - 1);
                        sbuf.append("{}");
                        nextIndex = delimiterIndex + 2;
                    }
                } else {
                    sbuf.append(messagePattern, nextIndex, delimiterIndex);
                    appendPrintfPlaceholder(sbuf);
                    nextIndex = delimiterIndex + 2;
                }
            }
        }
    }

    private static void appendPrintfPlaceholder(StringBuilder sbuf) {
        sbuf.append("%s");
    }

    private static boolean isEscapedDelimeter(String messagePattern, int delimeterStartIndex) {

        if (delimeterStartIndex == 0) {
            return false;
        }
        char potentialEscape = messagePattern.charAt(delimeterStartIndex - 1);
        return potentialEscape == ESCAPE_CHAR;
    }

    private static boolean isDoubleEscaped(String messagePattern, int delimeterStartIndex) {
        return delimeterStartIndex >= 2 && messagePattern.charAt(delimeterStartIndex - 2) == ESCAPE_CHAR;
    }
}
