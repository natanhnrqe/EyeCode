package com.eyecode.learning.markdown;

import java.util.Collections;
import java.util.List;

public final class MarkdownDocument {

    private final List<MarkdownNode> nodes;

    public MarkdownDocument(List<MarkdownNode> nodes) {
        this.nodes = nodes != null
                ? List.copyOf(nodes)
                : Collections.emptyList();
    }

    public List<MarkdownNode> nodes() {
        return nodes;
    }
}
