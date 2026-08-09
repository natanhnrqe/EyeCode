package com.eyecode.language.java.incremental;

import com.eyecode.language.java.LexerSnapshot;

/**
 * Outcome of an incremental lexing pass.
 * <p>
 * The {@link #snapshot()} is the authoritative result; the counters exist to
 * demonstrate how much work was actually reused and to make the incremental
 * path observable in tests.
 */
public final class IncrementalLexResult {

    private final LexerSnapshot snapshot;
    private final boolean incremental;
    private final int relexedTokenCount;
    private final int prefixReusedTokenCount;
    private final TokenReuseWindow reuseWindow;

    public IncrementalLexResult(LexerSnapshot snapshot,
                                boolean incremental,
                                int relexedTokenCount,
                                int prefixReusedTokenCount,
                                TokenReuseWindow reuseWindow) {
        this.snapshot = snapshot;
        this.incremental = incremental;
        this.relexedTokenCount = relexedTokenCount;
        this.prefixReusedTokenCount = prefixReusedTokenCount;
        this.reuseWindow = reuseWindow == null
                ? new TokenReuseWindow(0, 0, 0)
                : reuseWindow;
    }

    public LexerSnapshot snapshot() {
        return snapshot;
    }

    /**
     * True when the incremental path reused at least part of the previous
     * token stream; false when the full re-lex fallback ran.
     */
    public boolean isIncremental() {
        return incremental;
    }

    public int relexedTokenCount() {
        return relexedTokenCount;
    }

    public int prefixReusedTokenCount() {
        return prefixReusedTokenCount;
    }

    public TokenReuseWindow reuseWindow() {
        return reuseWindow;
    }

    public int totalReusedTokenCount() {
        return prefixReusedTokenCount + reuseWindow.reusedTokenCount();
    }
}
