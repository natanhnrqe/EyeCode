package com.eyecode.language.java.parser.incremental;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;

/**
 * Description of the structural region affected by a {@link
 * com.eyecode.editor.intelligence.document.TextChange}.
 * <p>
 * Produced by {@link ParserChangeAnalyzer} as a triage step: before the
 * incremental parser decides what to reparse, the analyzer picks the
 * smallest structural region that can be safely rebuilt and exposes it
 * here. When the change is too ambiguous for safe incremental work
 * (boundary edits, missing AST, multiple methods affected), the region
 * carries {@link #fallbackReason()} set and {@link #fallbackRequired()}
 * is {@code true} — the parser service should then take the full reparse
 * path.
 *
 * @param oldRange          range that was removed in the old text
 * @param newRange          range that was inserted in the new text
 * @param affectedNode      deepest AST node containing the change (or {@code null}
 *                          when no node matches safely)
 * @param reparsableRange   range in the NEW text that the incremental
 *                          parser may safely rebuild (a single
 *                          statement-level region, an enclosing block, an
 *                          enclosing method, or the whole compilation unit)
 * @param fallbackRequired  {@code true} when the change must trigger a
 *                          full reparse
 * @param fallbackReason    short, human-readable reason for the fallback
 *                          ({@code null} when {@code fallbackRequired} is
 *                          {@code false})
 */
public record ParserChangeRegion(
        TextRange oldRange,
        TextRange newRange,
        AstNode affectedNode,
        TextRange reparsableRange,
        boolean fallbackRequired,
        String fallbackReason) {

    /**
     * A region that always forces a full reparse.
     */
    public static ParserChangeRegion fullReparse(TextRange oldRange, TextRange newRange,
                                                  AstNode affectedNode, String reason) {
        return new ParserChangeRegion(oldRange, newRange, affectedNode, newRange,
                true, reason);
    }

    /**
     * A region that can be incrementally rebuilt inside {@code reparsableRange}.
     */
    public static ParserChangeRegion incremental(TextRange oldRange, TextRange newRange,
                                                 AstNode affectedNode, TextRange reparsableRange) {
        return new ParserChangeRegion(oldRange, newRange, affectedNode, reparsableRange,
                false, null);
    }
}
