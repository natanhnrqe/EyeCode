package com.eyecode.runtime;

public record RunOutputChunk(String text, boolean error) {
    public RunOutputChunk {
        if (text == null) {
            throw new IllegalArgumentException("text must not be null");
        }
    }
}
