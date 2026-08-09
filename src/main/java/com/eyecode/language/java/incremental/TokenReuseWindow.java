package com.eyecode.language.java.incremental;

/**
 * Describes the span of old tokens reused by an incremental lexing pass.
 *
 * @param firstReusedTokenIndex index (in the old token list) of the first reused token
 * @param offsetDelta           offset shift applied to every reused token
 * @param reusedTokenCount      number of old tokens reused (0 when nothing was reused)
 */
public record TokenReuseWindow(int firstReusedTokenIndex, int offsetDelta, int reusedTokenCount) {

    public boolean isEmpty() {
        return reusedTokenCount <= 0;
    }
}
