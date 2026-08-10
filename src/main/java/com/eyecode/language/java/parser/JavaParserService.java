package com.eyecode.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.java.parser.incremental.FullReparseStrategy;
import com.eyecode.language.java.parser.incremental.IncrementalParserStrategy;

/**
 * Default {@link ParserService} implementation.
 * <p>
 * Maintains a single-slot cache (per JVM instance) of the last
 * {@link ParserSnapshot} per session id; for anonymous one-shot snapshots
 * (no session id) the service always falls back to a full reparse.
 * For session snapshots, the service tries the
 * {@link IncrementalParserStrategy} first; when the strategy declines
 * (fallback required) or fails, a {@link FullReparseStrategy} reparses
 * the whole snapshot.
 * <p>
 * Newest-wins: a snapshot with an older version than the cached entry
 * cannot overwrite it. The implementation still produces a snapshot for
 * the older version (callers may legitimately want it), but the cache
 * advances only forward.
 */
public final class JavaParserService implements ParserService {

    private final IncrementalParserStrategy incremental = new IncrementalParserStrategy();
    private final FullReparseStrategy fullReparse = new FullReparseStrategy();

    private String cachedSessionId;
    private long cachedVersion;
    private ParserSnapshot cachedSnapshot;

    @Override
    public synchronized ParserSnapshot parse(DocumentSnapshot document) {
        if (document == null) {
            throw new IllegalArgumentException("document must not be null");
        }
        if (document.sessionId() == null) {
            AstNode fresh = fullReparse.reparse(document);
            return new ParserSnapshot(document.version(), document.getText(), fresh);
        }
        if (isCacheHit(document)) {
            return cachedSnapshot;
        }
        if (isCacheForSameSession(document)) {
            ParserSnapshot updated = incrementalParse(document);
            installIfNewer(document, updated);
            return updated;
        }
        AstNode fresh = fullReparse.reparse(document);
        ParserSnapshot snapshot = new ParserSnapshot(document.version(), document.getText(), fresh);
        installIfNewer(document, snapshot);
        return snapshot;
    }

    private boolean isCacheHit(DocumentSnapshot document) {
        return cachedSnapshot != null
                && document.sessionId().equals(cachedSessionId)
                && document.version() == cachedVersion;
    }

    private boolean isCacheForSameSession(DocumentSnapshot document) {
        return cachedSnapshot != null && document.sessionId().equals(cachedSessionId);
    }

    private ParserSnapshot incrementalParse(DocumentSnapshot document) {
        DocumentSnapshot previous = new DocumentSnapshot(
                cachedVersion,
                cachedSnapshot.text(),
                null,
                null,
                cachedSessionId);
        TextChange change = TextChange.between(previous, document);
        IncrementalParserStrategy.Result result = incremental.parse(document, previous,
                cachedSnapshot.astRoot(), change);
        if (result.fallbackUsed()) {
            AstNode fresh = fullReparse.reparse(document);
            return new ParserSnapshot(document.version(), document.getText(), fresh);
        }
        return new ParserSnapshot(document.version(), document.getText(), result.astRoot());
    }

    private void installIfNewer(DocumentSnapshot document, ParserSnapshot snapshot) {
        if (cachedSnapshot == null || snapshot.version() > cachedVersion) {
            cachedSessionId = document.sessionId();
            cachedVersion = snapshot.version();
            cachedSnapshot = snapshot;
        }
    }

    /**
     * Drops the cached snapshot entirely. Forces a full reparse on the
     * next {@link #parse(DocumentSnapshot)} for the same session.
     */
    public synchronized void invalidate() {
        cachedSessionId = null;
        cachedVersion = 0;
        cachedSnapshot = null;
    }

    /**
     * Returns the current cached snapshot, or {@code null} when nothing
     * has been parsed yet.
     */
    public synchronized ParserSnapshot cachedSnapshot() {
        return cachedSnapshot;
    }
}
