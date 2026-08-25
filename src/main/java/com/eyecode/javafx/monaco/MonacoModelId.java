package com.eyecode.javafx.monaco;

import com.eyecode.workbench.editor.EditorSession;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Optional;

public final class MonacoModelId {
    private MonacoModelId() {
    }

    public static String forSession(EditorSession session) {
        Path file = session.getFile();
        if (file != null) {
            return file.toAbsolutePath().normalize().toUri().toString();
        }
        return "eyecode://workspace/" + session.getDocumentId() + ".java";
    }

    public static Optional<Path> pathForModel(String modelId) {
        if (modelId == null || modelId.isBlank()) return Optional.empty();
        try {
            URI uri = URI.create(modelId);
            if (!"file".equalsIgnoreCase(uri.getScheme())) return Optional.empty();
            try {
                return Optional.of(Path.of(uri).toAbsolutePath().normalize());
            } catch (IllegalArgumentException ignored) {
                String decoded = URLDecoder.decode(uri.getRawPath().replace("+", "%2B"), StandardCharsets.UTF_8);
                if (decoded.length() > 3 && decoded.charAt(0) == '/' && decoded.charAt(2) == ':') {
                    decoded = decoded.substring(1);
                }
                return Optional.of(Path.of(decoded).toAbsolutePath().normalize());
            }
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }

    public static boolean matches(String modelId, Path sessionPath) {
        return pathForModel(modelId)
                .map(modelPath -> identity(modelPath).equals(identity(sessionPath)))
                .orElse(false);
    }

    public static String identity(Path path) {
        if (path == null) return "";
        String value = path.toAbsolutePath().normalize().toString().replace('/', '\\');
        if (value.length() > 1 && value.charAt(1) == ':') {
            value = Character.toUpperCase(value.charAt(0)) + value.substring(1);
        }
        return value.toLowerCase(Locale.ROOT);
    }
}
