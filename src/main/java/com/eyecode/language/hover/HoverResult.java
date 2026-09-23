package com.eyecode.language.hover;

import java.util.List;

public record HoverResult(List<HoverContent> contents, int rangeStart, int rangeEnd) {
    public HoverResult {
        contents = contents == null ? List.of() : List.copyOf(contents);
        rangeStart = Math.max(0, rangeStart);
        rangeEnd = Math.max(rangeStart, rangeEnd);
    }
}
