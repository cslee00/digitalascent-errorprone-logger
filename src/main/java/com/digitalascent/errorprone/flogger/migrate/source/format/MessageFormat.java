package com.digitalascent.errorprone.flogger.migrate.source.format;

import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatConversionFailedException;
import com.digitalascent.errorprone.flogger.migrate.source.format.MessageFormatConversionResult;
import com.sun.source.tree.ExpressionTree;

import java.lang.reflect.Field;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public final class MessageFormat {

    public static MessageFormatConversionResult convertJavaTextMessageFormat(String sourceMessageFormat,
                                                                             List<? extends ExpressionTree> formatArguments) {
        try {
            java.text.MessageFormat messageFormat = new java.text.MessageFormat(sourceMessageFormat);

            // wipe out formats to make everything string replacements
            resetFormats(messageFormat);

            // escape % signs as we convert to printf formatting
            messageFormat.applyPattern(messageFormat.toPattern().replace("%", "%%"));

            // determine the number of arguments
            int[] argumentNumbers = determineArgumentNumbers(messageFormat);

            int max = Arrays.stream(argumentNumbers).max().getAsInt() + 1;

            int maxOffset = determineMaxOffset(messageFormat);

            List<ExpressionTree> argumentList = new ArrayList<>();
            List<String> migrationIssues = new ArrayList<>();
            for (int i = 0; i <= maxOffset; i++) {
                int idx = argumentNumbers[i];
                if (idx < formatArguments.size()) {
                    argumentList.add(formatArguments.get(idx));
                } else {
                    migrationIssues.add("Invalid parameter index: " + idx + ": \"" + messageFormat.toPattern() + "\"");
                }
            }

            for (ExpressionTree remainingArgument : formatArguments) {
                if (!argumentList.contains(remainingArgument)) {
                    migrationIssues.add("Unused parameter: " + remainingArgument.toString().replace("\r","\\r").replace("\n","\\n") );
                    argumentList.add(remainingArgument);
                }
            }

            // replace placeholders with printf format specifiers
            Object[] args = new Object[max];
            Arrays.fill(args, "%s");
            String convertedFormatString = messageFormat.format(args);
            return new MessageFormatConversionResult(convertedFormatString, argumentList, migrationIssues);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new MessageFormatConversionFailedException(e.getMessage());
        }
    }

    private static int[] determineArgumentNumbers(java.text.MessageFormat messageFormat) throws NoSuchFieldException, IllegalAccessException {
        Field argumentNumbersField = createField(messageFormat, "argumentNumbers");
        return (int[]) argumentNumbersField.get(messageFormat);
    }

    private static int determineMaxOffset(java.text.MessageFormat messageFormat) throws NoSuchFieldException, IllegalAccessException {
        Field maxOffsetField = createField(messageFormat, "maxOffset");
        return (int) maxOffsetField.get(messageFormat);
    }

    private static void resetFormats(java.text.MessageFormat messageFormat) throws NoSuchFieldException, IllegalAccessException {
        Field formatsField = createField(messageFormat, "formats");
        Format[] formats = (Format[]) formatsField.get(messageFormat);
        Arrays.fill(formats, null);
    }

    private static Field createField(java.text.MessageFormat messageFormat, String fieldName) throws NoSuchFieldException {
        Field field = messageFormat.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field;
    }

    private MessageFormat() {
        throw new AssertionError("Cannot instantiate: " + getClass());
    }
}
