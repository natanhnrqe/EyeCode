package com.eyecode.language.java;

/**
 * Lifecycle state of a lexer cache entry.
 * <p>
 * ACTIVE entries may keep the incremental structures needed for the next
 * incremental re-lex (the previous text plus the previous snapshot) and answer
 * immediately. INACTIVE entries keep only the last known snapshot and version
 * (minimal metadata for invalidation/rebuild) — heavy incremental structures
 * must be released; the next access rebuilds what it needs.
 */
public enum LexerSessionState {

    ACTIVE,
    INACTIVE;
}
