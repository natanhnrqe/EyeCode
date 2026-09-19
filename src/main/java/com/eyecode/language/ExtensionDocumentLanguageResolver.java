package com.eyecode.language;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class ExtensionDocumentLanguageResolver implements DocumentLanguageResolver {
    private final Map<String, LanguageId> languagesByExtension;

    public ExtensionDocumentLanguageResolver(Map<LanguageId, Set<String>> extensionsByLanguage) {
        Map<String, LanguageId> registered = new LinkedHashMap<>();
        for (Map.Entry<LanguageId, Set<String>> entry : extensionsByLanguage.entrySet()) {
            LanguageId language = entry.getKey();
            for (String extension : entry.getValue()) {
                registered.put(normalizeExtension(extension), language);
            }
        }
        languagesByExtension = Map.copyOf(registered);
    }

    @Override
    public Optional<LanguageId> resolve(LanguageDocument document) {
        if (document == null) return Optional.empty();
        if (document.declaredLanguage() != null) {
            return Optional.of(document.declaredLanguage());
        }
        if (document.sourceFile() != null) {
            Optional<LanguageId> resolved = fromName(document.sourceFile().getFileName().toString());
            if (resolved.isPresent()) return resolved;
        }
        Optional<LanguageId> displayName = fromName(document.displayName());
        if (displayName.isPresent()) return displayName;
        return fromName(document.uri());
    }

    private Optional<LanguageId> fromName(String value) {
        if (value == null) return Optional.empty();
        int dot = value.lastIndexOf('.');
        if (dot < 0 || dot == value.length() - 1) return Optional.empty();
        return Optional.ofNullable(languagesByExtension.get(value.substring(dot + 1).toLowerCase(Locale.ROOT)));
    }

    private static String normalizeExtension(String extension) {
        String value = extension == null ? "" : extension.trim().toLowerCase(Locale.ROOT);
        return value.startsWith(".") ? value.substring(1) : value;
    }
}
