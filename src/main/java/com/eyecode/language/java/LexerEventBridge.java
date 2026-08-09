package com.eyecode.language.java;

import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.SubscriptionToken;
import com.eyecode.language.java.event.TokensUpdatedEvent;

/**
 * Bridges document mutations to lexical events on an {@link EventBus}.
 * <p>
 * Subscribes to {@link DocumentTextChangeEvent} and, for each committed
 * mutation, lexes the exact after-snapshot carried by the event and publishes
 * a {@link TokensUpdatedEvent}. The lexed snapshot version always equals the
 * document version at mutation time — the bridge never invents a version.
 * <p>
 * Synchronous by design (Sprint 5.2b): no threads, no debounce, no scheduler.
 */
public final class LexerEventBridge {

    private final LexerService lexerService;
    private final EventBus eventBus;
    private final SubscriptionToken subscription;

    public LexerEventBridge(LexerService lexerService, EventBus eventBus) {
        if (lexerService == null) {
            throw new IllegalArgumentException("lexerService must not be null");
        }
        if (eventBus == null) {
            throw new IllegalArgumentException("eventBus must not be null");
        }
        this.lexerService = lexerService;
        this.eventBus = eventBus;
        this.subscription = eventBus.subscribe(DocumentTextChangeEvent.class, this::onDocumentChanged);
    }

    private void onDocumentChanged(DocumentTextChangeEvent event) {
        LexerSnapshot snapshot = lexerService.lex(event.getAfter());
        eventBus.publish(new TokensUpdatedEvent(snapshot));
    }

    public void dispose() {
        eventBus.unsubscribe(subscription);
    }
}
