package com.eyecode.language;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.eventbus.EventBus;
import com.eyecode.language.java.JavaTokenType;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.event.TokensUpdatedEvent;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TokensUpdatedEventTest {

    private static Token token(JavaTokenType type, int start, int end, String text) {
        return new Token(type, TextRange.of(start, end), text);
    }

    private static LexerSnapshot snapshot(long version) {
        return new LexerSnapshot(version, List.of(
                token(JavaTokenType.KEYWORD, 0, 5, "class"),
                token(JavaTokenType.IDENTIFIER, 6, 10, "Test"),
                token(JavaTokenType.EOF, 10, 10, "")
        ));
    }

    @Test
    void carriesTheCorrectSnapshot() {
        LexerSnapshot snapshot = snapshot(4);
        TokensUpdatedEvent event = new TokensUpdatedEvent(snapshot);

        assertSame(snapshot, event.getSnapshot());
    }

    @Test
    void exposesTheSnapshotVersion() {
        TokensUpdatedEvent event = new TokensUpdatedEvent(snapshot(12));

        assertEquals(12, event.getVersion());
        assertEquals(12, event.getSnapshot().version());
    }

    @Test
    void tokensMatchTheSnapshot() {
        TokensUpdatedEvent event = new TokensUpdatedEvent(snapshot(2));

        assertEquals(event.getSnapshot().tokens(), event.getSnapshot().tokens());
        assertEquals(3, event.getSnapshot().tokens().size());
        assertEquals(token(JavaTokenType.KEYWORD, 0, 5, "class"),
                event.getSnapshot().tokens().get(0));
    }

    @Test
    void rejectsNullSnapshot() {
        assertThrows(IllegalArgumentException.class, () -> new TokensUpdatedEvent(null));
    }

    @Test
    void publishedEventsKeepPublishOrder() {
        EventBus bus = new EventBus();
        List<TokensUpdatedEvent> received = new ArrayList<>();
        bus.subscribe(TokensUpdatedEvent.class, received::add);

        bus.publish(new TokensUpdatedEvent(snapshot(1)));
        bus.publish(new TokensUpdatedEvent(snapshot(2)));
        bus.publish(new TokensUpdatedEvent(snapshot(3)));

        assertEquals(3, received.size());
        assertEquals(1, received.get(0).getVersion());
        assertEquals(2, received.get(1).getVersion());
        assertEquals(3, received.get(2).getVersion());
    }

    @Test
    void olderEventIsNeverAlteredByLaterPublish() {
        TokensUpdatedEvent first = new TokensUpdatedEvent(snapshot(1));
        TokensUpdatedEvent second = new TokensUpdatedEvent(snapshot(2));

        assertEquals(1, first.getVersion());
        assertEquals(2, second.getVersion());
        assertNotSame(first.getSnapshot(), second.getSnapshot());

        assertEquals(snapshot(1).tokens(), first.getSnapshot().tokens());
        assertEquals(1, first.getSnapshot().version());
    }
}
