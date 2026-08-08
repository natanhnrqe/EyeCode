package com.eyecode.editor.intelligence.indent;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;

/**
 * Language-aware indentation rules consulted by the smart editing strategies
 * (auto-indent, smart enter).
 * <p>
 * A policy is pure Core: it reads text only through {@link DocumentSnapshot}s
 * and returns plain indentation levels and whitespace strings. It contains no
 * editing logic — applying the computed indentation is the caller's job.
 */
public interface IndentPolicy {

    int indentSize();

    String indentationFor(int level);

    int indentationLevel(String text, int line);

    int currentLineIndentLevel(DocumentSnapshot snapshot, int line);

    int nextLineIndentLevel(DocumentSnapshot snapshot, int line);

    boolean shouldDedent(DocumentSnapshot snapshot, int line);
}
