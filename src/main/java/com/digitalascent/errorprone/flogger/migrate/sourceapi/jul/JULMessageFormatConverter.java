package com.digitalascent.errorprone.flogger.migrate.sourceapi.jul;

/**
 * Converts SLF4J parameter placeholder '{}' into printf-style parameter '%s'.
 * Based on SLF4J org.slf4j.helpers.MessageFormatter to ensure accuracy of parsing
 */
final class JULMessageFormatConverter {

    private static final char DELIM_START = '{';
    private static final char DELIM_STOP = '}';
    private static final String DELIM_STR = "{}";
    private static final char ESCAPE_CHAR = '\\';

    static String convertMessageFormat(String messagePattern ) {

        if (messagePattern == null) {
            return null;
        }

        // escape '%' sign in incoming format
        messagePattern = messagePattern.replace("%", "%%");

        int nextIndex = 0;
        int delimiterIndex;
        StringBuilder sbuf = new StringBuilder(messagePattern.length() + 50);

        int argumentIndex;
        while( true ) {
            delimiterIndex = messagePattern.indexOf(DELIM_STR, nextIndex);

            if (delimiterIndex == -1) {
                // no more variables
                if (nextIndex == 0) { // this is a simple string
                    return messagePattern;
                } else { // add the tail string which contains no variables and return
                    // the result.
                    sbuf.append(messagePattern, nextIndex, messagePattern.length());
                    return sbuf.toString();
                }
            } else {
                if (isEscapedDelimeter(messagePattern, delimiterIndex)) {
                    if (isDoubleEscaped(messagePattern, delimiterIndex)) {
                        // The escape character preceding the delimiter start is
                        // itself escaped: "abc x:\\{}"
                        // we have to consume one backward slash
                        sbuf.append(messagePattern, nextIndex, delimiterIndex - 1 );
                        appendPrintfPlaceholder(sbuf);
                        nextIndex = delimiterIndex + 2;
                    } else {
                        sbuf.append(messagePattern, nextIndex, delimiterIndex - 1);
                        sbuf.append("{}");
                        nextIndex = delimiterIndex + 2;
                    }
                } else {
                    // normal case
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
