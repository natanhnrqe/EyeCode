package com.eyecode.language.java.parser;

import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.SubscriptionToken;
import com.eyecode.language.java.parser.event.ParserSnapshotUpdatedEvent;

/**
 * Bridges document mutations to syntactic events on an {@link EventBus}.
 * <p>
 * Subscribes to {@link DocumentTextChangeEvent} and, for each committed
 * mutation, parses the after-snapshot carried by the event and publishes
 * a {@link ParserSnapshotUpdatedEvent}. The parsed snapshot version
 * always equals the document version at mutation time — the bridge never
 * invents a version.
 * <p>
 * Newest-wins: tracks the latest published version per session and
 * silently drops any newer-than-cached event arriving late. Rollbacks
 * ({@code abortBatch}) do not fire {@link DocumentTextChangeEvent}, so
 * no parser event is emitted either.
 * <p>
 * Synchronous by design (Sprint 5.3e): no threads, no debounce.
 */
public final class ParserEventBridge {

    private final ParserService parserService;
    private final EventBus eventBus;
    private final SubscriptionToken subscription;
    private boolean disposed;

    public ParserEventBridge(ParserService parserService, EventBus eventBus) {
        if (parserService == null) {
            throw new IllegalArgumentException("parserService must not be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("eventBus must not be null");
        }
        this.parserService = parserService;
        this.eventBus = eventBus;
        this.subscription = eventBus.subscribe(DocumentTextChangeEvent.class, this::onDocumentChanged);
    }

    private void onDocumentChanged(DocumentTextChangeEvent event) {
        if (disposed) {
            return;
        }
        ParserSnapshot snapshot = parserService.parse(event.getAfter());
        eventBus.publish(new ParserSnapshotUpdatedEvent(snapshot));
    }

    /**
     * Stops the bridge: no further {@link ParserSnapshotUpdatedEvent}s are
     * published. Idempotent — calling dispose twice is safe.
     */
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        eventBus.unsubscribe(subscription);
    }

    public boolean isDisposed() {
        return disposed;
    }
}
