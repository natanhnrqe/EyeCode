package com.eyecode.language.navigation;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

public record NavigationTarget(String uri, TextRange range, TextRange selectionRange) {

    public NavigationTarget {
        Objects.requireNonNull(uri, "uri must not be null");
        Objects.requireNonNull(range, "range must not be null");
        Objects.requireNonNull(selectionRange, "selectionRange must not be null");
    }

    public static NavigationTarget of(String uri, TextRange range) {
        return new NavigationTarget(uri, range, range);
    }
}
