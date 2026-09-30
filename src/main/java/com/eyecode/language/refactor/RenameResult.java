package com.eyecode.language.refactor;

import java.util.List;

public record RenameResult(boolean success, int applied, List<String> failedFiles) {
    public static RenameResult success(int applied) {
        return new RenameResult(true, applied, List.of());
    }

    public static RenameResult failed(int applied, List<String> failedFiles) {
        return new RenameResult(false, applied, List.copyOf(failedFiles));
    }
}
