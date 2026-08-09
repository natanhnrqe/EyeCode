package com.eyecode.language.java;

import com.eyecode.language.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Immutable, versioned result of a lexical analysis.
 * <p>
 * A snapshot is bound to the exact {@link #version()} of the document it was
 * produced from and never changes after creation: the version cannot be
 * altered and the token list is defensively copied and exposed unmodifiable.
 * A stale snapshot stays valid for the version it describes — it is never
 * silently updated when the document advances.
 */
public final class LexerSnapshot {

    private final long version;
    private final List<Token> tokens;

    public LexerSnapshot(long version, List<Token> tokens) {
        if (tokens == null) {
            throw new IllegalArgumentException("tokens must not be null");
        }
        this.version = version;
        List<Token> copy = new ArrayList<>(tokens);
        for (Token token : copy) {
            Objects.requireNonNull(token, "tokens must not contain null");
        }
        this.tokens = Collections.unmodifiableList(copy);
    }

    public long version() {
        return version;
    }

    public List<Token> tokens() {
        return tokens;
    }

    public boolean isEmpty() {
        return tokens.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof LexerSnapshot that)) return false;
        return version == that.version && tokens.equals(that.tokens);
    }

    @Override
    public int hashCode() {
        return Objects.hash(version, tokens);
    }

    @Override
    public String toString() {
        return "LexerSnapshot{" + "version=" + version + ", tokens=" + tokens.size() + '}';
    }
}
