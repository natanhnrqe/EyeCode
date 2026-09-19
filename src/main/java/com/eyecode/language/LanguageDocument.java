package com.eyecode.language;

import java.nio.file.Path;

public record LanguageDocument(String uri, Path sourceFile, String displayName, LanguageId declaredLanguage) {
    public LanguageDocument {
        uri = uri == null ? "" : uri;
        displayName = displayName == null ? "" : displayName;
    }
}
