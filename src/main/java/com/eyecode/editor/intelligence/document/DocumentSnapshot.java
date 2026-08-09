package com.eyecode.editor.intelligence.document;

import java.nio.file.Path;

/**
 * Immutable, versioned view of a document's content.
 * <p>
 * A snapshot never changes after creation: subsequent edits to the underlying
 * document produce a new snapshot with a higher version. All intelligence
 * subsystems (lexer, parser, semantic, completion, hover, AI) must read text
 * exclusively through snapshots.
 * <p>
 * A snapshot may carry a {@code sessionId} — the identity of the
 * document/session it belongs to (two sessions on the same file have distinct
 * identities). {@code null} marks an anonymous, one-shot snapshot (no session;
 * per-version caching must not be applied).
 */
public final class DocumentSnapshot {

    private final long version;
    private final String text;
    private final LineMap lineMap;
    private final Path sourceFile;
    private final String sessionId;

    public DocumentSnapshot(long version, String text, LineMap lineMap, Path sourceFile) {
        this(version, text, lineMap, sourceFile, null);
    }

    public DocumentSnapshot(long version, String text, LineMap lineMap, Path sourceFile,
                            String sessionId) {
        this.version = version;
        this.text = text == null ? "" : text;
        this.lineMap = lineMap != null ? lineMap : LineMap.of(this.text);
        this.sourceFile = sourceFile;
        this.sessionId = sessionId;
    }

    /**
     * Creates a version-less, session-less snapshot for one-shot lexical
     * analysis of standalone text (e.g. a file loaded from disk, indexer
     * passes). Version is fixed at 0 — no real document version exists — and
     * the absent {@code sessionId} makes the lexer treat it as an anonymous
     * one-shot: full re-lex, never cached. Do NOT use for open editor
     * documents; use {@code EditorDocument.snapshot()} there.
     */
    public static DocumentSnapshot oneShot(String text) {
        return new DocumentSnapshot(0, text, null, null, null);
    }

    /**
     * {@link #oneShot(String)} carrying a source file (for diagnostics).
     */
    public static DocumentSnapshot oneShot(String text, Path sourceFile) {
        return new DocumentSnapshot(0, text, null, sourceFile, null);
    }

    public long version() {
        return version;
    }

    public DocumentVersion documentVersion() {
        return new DocumentVersion(version);
    }

    public CharSequence text() {
        return text;
    }

    public String getText() {
        return text;
    }

    public int length() {
        return text.length();
    }

    public LineMap lineMap() {
        return lineMap;
    }

    public Path sourceFile() {
        return sourceFile;
    }

    /**
     * Identity of the document/session this snapshot belongs to, or
     * {@code null} for anonymous one-shot snapshots.
     */
    public String sessionId() {
        return sessionId;
    }

    public boolean isEmpty() {
        return text.isEmpty();
    }

    public String text(TextRange range) {
        if (range == null) return "";
        int start = Math.max(0, Math.min(range.startOffset(), text.length()));
        int end = Math.max(start, Math.min(range.endOffset(), text.length()));
        return text.substring(start, end);
    }
}
