package com.eyecode.editor.intelligence.document;

import java.nio.file.Path;

/**
 * Immutable, versioned view of a document's content.
 * <p>
 * A snapshot never changes after creation: subsequent edits to the underlying
 * document produce a new snapshot with a higher version. All intelligence
 * subsystems (lexer, parser, semantic, completion, hover, AI) must read text
 * exclusively through snapshots.
 */
public final class DocumentSnapshot {

    private final long version;
    private final String text;
    private final LineMap lineMap;
    private final Path sourceFile;

    public DocumentSnapshot(long version, String text, LineMap lineMap, Path sourceFile) {
        this.version = version;
        this.text = text == null ? "" : text;
        this.lineMap = lineMap != null ? lineMap : LineMap.of(this.text);
        this.sourceFile = sourceFile;
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
