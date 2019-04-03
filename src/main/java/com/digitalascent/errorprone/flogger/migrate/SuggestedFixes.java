package com.digitalascent.errorprone.flogger.migrate;


import com.google.errorprone.fixes.SuggestedFix;

import java.util.List;

final class SuggestedFixes {
    static SuggestedFix merge(List<SuggestedFix> suggestedFixes) {
        SuggestedFix.Builder fix = SuggestedFix.builder();
        suggestedFixes.forEach(fix::merge);
        return fix.build();
    }

    private SuggestedFixes() {
        throw new AssertionError("Cannot instantiate " + getClass());
    }
}
