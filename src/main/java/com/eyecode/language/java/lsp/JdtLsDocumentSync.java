package com.eyecode.language.java.lsp;

import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public final class JdtLsDocumentSync {
    private final Map<String, Integer> openVersions = new HashMap<>();
    private final Map<String, String> openTexts = new HashMap<>();

    public synchronized String synchronize(JdtLsSession session, Path file, String source, long version) {
        String uri = uriOf(file);
        Integer previous = openVersions.get(uri);
        if (previous == null) {
            session.didOpen(uri, source, lspOf(version));
            openVersions.put(uri, lspOf(version));
        } else if (!source.equals(openTexts.get(uri))) {
            int next = Math.max(previous + 1, lspOf(version));
            session.didChange(uri, source, next);
            openVersions.put(uri, next);
        }
        openTexts.put(uri, source);
        return uri;
    }

    synchronized long lastSyncedVersion(Path file) {
        return file == null ? -1 : lastSyncedVersion(uriOf(file));
    }

    synchronized long lastSyncedVersion(String uri) {
        return openVersions.getOrDefault(uri, -1);
    }

    public synchronized String textOf(String uri) {
        return uri == null ? null : openTexts.get(uri);
    }

    synchronized void close(JdtLsSession session, Path file) {        if (file == null) return;
        String uri = uriOf(file);
        openTexts.remove(uri);
        if (openVersions.remove(uri) != null) session.didClose(uri);
    }

    synchronized void reset() {
        openVersions.clear();
        openTexts.clear();
    }

    static String uriOf(Path file) {
        return file.toAbsolutePath().normalize().toUri().toString();
    }

    static int lspOf(long version) {
        return (int) Math.min(Integer.MAX_VALUE, Math.max(1L, version + 1));
    }
}
