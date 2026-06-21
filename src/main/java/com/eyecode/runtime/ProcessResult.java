package com.eyecode.runtime;

public record ProcessResult(
        int exitCode,
        String output,
        String error,
        long durationMs
) {
}
