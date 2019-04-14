package com.digitalascent.errorprone.flogger.migrate.source.api;

import com.digitalascent.errorprone.flogger.migrate.model.MethodInvocation;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class SourceApiUtil {

    private static final Pattern IS_ENABLED_LEVEL_PATTERN = Pattern.compile("^is(.*)Enabled$");

    public static String logLevelFromMethodName(MethodInvocation conditionaLoggingMethod ) {
        Matcher matcher = IS_ENABLED_LEVEL_PATTERN.matcher( conditionaLoggingMethod.methodName() );
        if( !matcher.matches() ) {
            throw new IllegalArgumentException("Cannot extract log level from: " + conditionaLoggingMethod);
        }
        return matcher.group(1);
    }

    private SourceApiUtil() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
