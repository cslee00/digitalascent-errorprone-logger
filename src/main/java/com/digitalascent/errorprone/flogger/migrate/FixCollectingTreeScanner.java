package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.List;

import static java.util.Objects.requireNonNull;

abstract class FixCollectingTreeScanner extends TreeScanner<Void, VisitorState> {
    private final List<SuggestedFix> suggestedFixes = new ArrayList<>();
    final void addSuggestedFix(SuggestedFix suggestedFix) {
        suggestedFixes.add( requireNonNull(suggestedFix, "suggestedFix") );
    }

    final List<SuggestedFix> suggestedFixes() {
        return ImmutableList.copyOf(suggestedFixes);
    }
}
