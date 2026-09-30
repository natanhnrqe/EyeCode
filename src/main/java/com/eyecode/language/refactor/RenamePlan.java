package com.eyecode.language.refactor;

import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public record RenamePlan(Map<Path, List<Edit>> editsByFile) {
    public RenamePlan {
        Objects.requireNonNull(editsByFile, "editsByFile");
        editsByFile.forEach((file, edits) -> {
            Objects.requireNonNull(file, "file");
            Objects.requireNonNull(edits, "edits");
            for (Edit edit : edits) Objects.requireNonNull(edit, "edit");
        });
        editsByFile = Collections.unmodifiableMap(new LinkedHashMap<>(editsByFile));
    }

    public boolean isEmpty() {
        return editsByFile.isEmpty() || editsByFile.values().stream().allMatch(List::isEmpty);
    }

    public record Edit(int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
        public Edit {
            if (startLine < 0 || startCharacter < 0 || endLine < startLine
                    || (endLine == startLine && endCharacter < startCharacter)) {
                throw new IllegalArgumentException("Invalid edit range");
            }
            Objects.requireNonNull(newText, "newText");
        }
    }
}
