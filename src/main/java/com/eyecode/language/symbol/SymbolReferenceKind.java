package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.TextRange;

/**
 * Kind of symbol reference (Sprint 5.4a).
 * <p>
 * Currently minimal — only the basic reference kind. Future sprints
 * may expand this to distinguish reads, writes, method calls, etc.
 */
public enum SymbolReferenceKind {
    /**
     * A reference whose kind is not yet classified or is a simple name usage.
     */
    SIMPLE
}