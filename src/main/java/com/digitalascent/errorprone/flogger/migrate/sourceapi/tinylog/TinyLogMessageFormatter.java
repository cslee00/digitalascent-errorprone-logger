package com.digitalascent.errorprone.flogger.migrate.sourceapi.tinylog;

import java.text.ChoiceFormat;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.Format;
import java.util.Locale;

public final class TinyLogMessageFormatter {

    private TinyLogMessageFormatter() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }


    private static final DecimalFormatSymbols FORMATTER_SYMBOLS = new DecimalFormatSymbols(Locale.ENGLISH);

    static String format(String message) {

        message = message.replace("%", "%%");

        StringBuilder builder = new StringBuilder(256);

        int start = 0;
        int openBraces = 0;

        for (int index = 0; index < message.length(); ++index) {
            char character = message.charAt(index);
            if (character == '{') {
                if (openBraces++ == 0 && start < index) {
                    builder.append(message, start, index);
                    start = index;
                }
            } else if (character == '}' && openBraces > 0) {
                if (--openBraces == 0) {
                    if (index == start + 1) {
                        builder.append("%s");
                    } else {
                        builder.append(format(message.substring(start + 1, index), "%s"));
                    }
                    start = index + 1;
                }
            }
        }

        if (start < message.length()) {
            builder.append(message, start, message.length());
        }

        return builder.toString();

    }

    private static String format(final String pattern, final Object argument) {
        try {
            return getFormatter(pattern, argument).format(argument);
        } catch (IllegalArgumentException ex) {
            return String.valueOf(argument);
        }
    }

    private static Format getFormatter(final String pattern, final Object argument) {
        if (pattern.indexOf('|') != -1) {
            int start = pattern.indexOf('{');
            if (start >= 0 && start < pattern.lastIndexOf('}')) {
                return new ChoiceFormat(format(pattern, new Object[]{argument}));
            } else {
                return new ChoiceFormat(pattern);
            }
        } else {
            return new DecimalFormat(pattern, FORMATTER_SYMBOLS);
        }
    }

}
