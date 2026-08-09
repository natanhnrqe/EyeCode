package com.eyecode.language.ast;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.Objects;

/**
 * Value object describing a syntax problem at a document range (Sprint 5.3a).
 * <p>
 * Pure Core — the v2 diagnostics layer (and later the diagnostics engine)
 * can map these onto its own representations. No recovery strategy change is
 * implied: this sprint only introduces the representation.
 */
public record SyntaxError(TextRange range, String message) {

    public SyntaxError {
        Objects.requireNonNull(range, "range must not be null");
        Objects.requireNonNull(message, "message must not be null");
    }
}
