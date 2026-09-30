package com.eyecode.language.java.lsp;

import java.util.Objects;

public record JdtLsTextEdit(int startLine, int startCharacter, int endLine, int endCharacter, String newText) {
    public JdtLsTextEdit {
        newText = Objects.requireNonNullElse(newText, "");
    }
}
