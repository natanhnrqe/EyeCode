package com.eyecode.editor.intelligence.events;

import com.eyecode.editor.intelligence.document.DocumentTransaction;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.eventbus.Event;
import com.eyecode.eventbus.EventBus;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DocumentEventsTest {

    @Test
    void bufferPublishesChangeEventsToEventBus() {
        EventBus bus = new EventBus();
        List<DocumentTextChangeEvent> received = new ArrayList<>();
        bus.subscribe(DocumentTextChangeEvent.class, received::add);

        EditorDocument document = new EditorDocument();
        new EditorBuffer(document, bus);
        document.insert(0, "abc");

        assertEquals(1, received.size());
        assertEquals("abc", received.get(0).getAfter().getText());
        assertEquals("", received.get(0).getBefore().getText());
        assertFalse(received.get(0).isTransactional());
    }

    @Test
    void eventBusDispatchesExactClassOnly() {
        EventBus bus = new EventBus();
        List<Event> supertypeReceived = new ArrayList<>();
        bus.subscribe(Event.class, supertypeReceived::add);

        EditorDocument document = new EditorDocument();
        new EditorBuffer(document, bus);
        document.insert(0, "x");

        assertTrue(supertypeReceived.isEmpty());
    }

    @Test
    void transactionPublishesSingleMergedEvent() {
        EventBus bus = new EventBus();
        List<DocumentTextChangeEvent> received = new ArrayList<>();
        bus.subscribe(DocumentTextChangeEvent.class, received::add);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document, bus);
        try (DocumentTransaction tx = DocumentTransaction.open(document, buffer.getCommandManager())) {
            tx.insert(0, "Hello");
            tx.insert(5, " World");
            tx.commit();
        }

        assertEquals(1, received.size());
        DocumentTextChangeEvent event = received.get(0);
        assertTrue(event.isTransactional());
        assertEquals("Hello World", event.getAfter().getText());
        assertEquals("Hello World", event.getChange().insertedText());
        assertEquals(new TextRange(0, 11), event.getChange().resultingRange());
    }

    @Test
    void bufferDoesNotRecordHistoryForTransactionalEvents() {
        EventBus bus = new EventBus();
        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document, bus);

        try (DocumentTransaction tx = DocumentTransaction.open(document, buffer.getCommandManager())) {
            tx.insert(0, "data");
            tx.commit();
        }

        assertTrue(buffer.canUndo());
        buffer.undo();
        assertEquals("", document.getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void documentFiresPerMutationEventsToListeners() {
        EditorDocument document = new EditorDocument();
        List<DocumentTextChangeEvent> received = new ArrayList<>();
        document.addDocumentChangeListener(received::add);

        document.insert(0, "a");
        document.insert(1, "b");

        assertEquals(2, received.size());
        assertEquals("a", received.get(0).getAfter().getText());
        assertEquals("ab", received.get(1).getAfter().getText());
    }

    @Test
    void removingListenerStopsDelivery() {
        EditorDocument document = new EditorDocument();
        List<DocumentTextChangeEvent> received = new ArrayList<>();
        DocumentChangeListener listener = received::add;
        document.addDocumentChangeListener(listener);
        document.removeDocumentChangeListener(listener);

        document.insert(0, "a");

        assertTrue(received.isEmpty());
    }
}
