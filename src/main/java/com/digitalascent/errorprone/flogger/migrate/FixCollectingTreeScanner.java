package com.digitalascent.errorprone.flogger.migrate;

import com.google.common.collect.ImmutableList;
import com.google.errorprone.VisitorState;
import com.google.errorprone.fixes.SuggestedFix;
import com.sun.source.util.TreeScanner;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@SuppressWarnings("OptionalUsedAsFieldOrParameterType")
abstract class FixCollectingTreeScanner extends TreeScanner<Void, VisitorState> {
    private final List<SuggestedFix> suggestedFixes = new ArrayList<>();
    protected final void addSuggestedFix( Optional<SuggestedFix> suggestedFix ) {
        suggestedFix.ifPresent(suggestedFixes::add);
    }

    final List<SuggestedFix> suggestedFixes() {
        return ImmutableList.copyOf(suggestedFixes);
    }
}
