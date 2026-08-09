package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.eventbus.EventBus;
import com.eyecode.language.java.JavaLexer;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.java.LexerEventBridge;
import com.eyecode.language.java.LexerSnapshot;
import com.eyecode.language.java.event.TokensUpdatedEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LexerEventBridgeIntegrationTest {

    private EditorDocument document;
    private EventBus eventBus;
    private LexerEventBridge bridge;
    private List<TokensUpdatedEvent> received;

    @BeforeEach
    void setUp() {
        document = new EditorDocument(null, "class Test {}");
        eventBus = new EventBus();
        new EditorBuffer(document, eventBus);
        bridge = new LexerEventBridge(new JavaLexerService(), eventBus);
        received = new ArrayList<>();
        eventBus.subscribe(TokensUpdatedEvent.class, received::add);
    }

    private static LexerSnapshot lexed(long version, String text) {
        return new JavaLexerService().lex(new DocumentSnapshot(version, text, null, null));
    }

    private static List<Token> directTokenize(String source) {
        return new JavaLexer().tokenize(source);
    }

    @Test
    void mutationPublishesVersionedTokensEvent() {
        assertEquals(1, document.currentVersion());

        document.insert(11, "\n");
        document.insert(0, "import java.util.List;\n");

        assertEquals(2, received.size());
        assertEquals(2, received.get(0).getVersion());
        assertEquals(3, received.get(1).getVersion());
        assertEquals(lexed(3, document.getText()), received.get(1).getSnapshot());
    }

    @Test
    void snapshotVersionMatchesDocumentVersionAtMutationTime() {
        document.insert(11, "\n");
        assertEquals(document.currentVersion(), received.get(0).getVersion());

        document.insert(0, "// head\n");
        assertEquals(document.currentVersion(), received.get(1).getVersion());

        document.insert(12, "\n");
        assertEquals(document.currentVersion(), received.get(2).getVersion());
    }

    @Test
    void tokensCorrespondToTheMutatedText() {
        document.insert(11, "\n");
        document.insert(0, "import java.util.List;\n");

        String finalText = document.getText();
        assertEquals(directTokenize(finalText), received.get(1).getSnapshot().tokens());
    }

    @Test
    void olderSnapshotIsNeverMutatedByLaterMutations() {
        document.insert(11, "\n");
        LexerSnapshot first = received.get(0).getSnapshot();
        String textAtFirstMutation = "class Test \n{}";

        document.insert(0, "// head\n");

        assertEquals(2, first.version());
        assertEquals(directTokenize(textAtFirstMutation), first.tokens());
        assertNotEquals(received.get(0).getSnapshot(), received.get(1).getSnapshot());
    }

    @Test
    void batchProducesSingleMergedTokensEvent() {
        document.beginBatch();
        document.insert(11, "\n");
        document.insert(0, "import x;\n");
        document.endBatch();

        assertEquals(1, received.size());
        assertEquals(document.currentVersion(), received.get(0).getVersion());
        assertEquals(lexed(document.currentVersion(), document.getText()),
                received.get(0).getSnapshot());
    }

    @Test
    void disposeStopsEventDelivery() {
        document.insert(11, "\n");
        assertEquals(1, received.size());

        bridge.dispose();
        document.insert(0, "// x\n");

        assertEquals(1, received.size());
    }

    @Test
    void rejectsNullServiceOrBus() {
        assertThrows(IllegalArgumentException.class,
                () -> new LexerEventBridge(null, eventBus));
        assertThrows(IllegalArgumentException.class,
                () -> new LexerEventBridge(new JavaLexerService(), null));
    }
}
