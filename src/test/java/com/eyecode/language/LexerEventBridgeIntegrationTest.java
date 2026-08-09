package com.eyecode.language;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.eventbus.EventBus;
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
import static org.junit.jupiter.api.Assertions.assertTrue;

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
        assertEquals(lexed(received.get(1).getVersion(), finalText).tokens(),
                received.get(1).getSnapshot().tokens());
    }

    @Test
    void olderSnapshotIsNeverMutatedByLaterMutations() {
        document.insert(11, "\n");
        LexerSnapshot first = received.get(0).getSnapshot();
        String textAtFirstMutation = "class Test \n{}";

        document.insert(0, "// head\n");

        assertEquals(2, first.version());
        assertEquals(lexed(2, textAtFirstMutation).tokens(), first.tokens());
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
    void everyEventSnapshotEqualsAFreshFullRelexOfTheText() {
        document.insert(11, "\n");
        String t1 = document.getText();
        document.insert(0, "// head\n");
        String t2 = document.getText();
        document.insert(12, "int x = 1;");
        String t3 = document.getText();

        assertEquals(3, received.size());
        assertEquals(lexed(2, t1), received.get(0).getSnapshot());
        assertEquals(lexed(3, t2), received.get(1).getSnapshot());
        assertEquals(lexed(4, t3), received.get(2).getSnapshot());
    }

    @Test
    void rejectNullServiceOrBus() {
        assertThrows(IllegalArgumentException.class,
                () -> new LexerEventBridge(null, eventBus));
        assertThrows(IllegalArgumentException.class,
                () -> new LexerEventBridge(new JavaLexerService(), null));
    }

    @Test
    void versionsAreStrictlyMonotonicAcrossRapidEdits() {
        document.insert(11, "\n");
        document.insert(0, "int a;\n");
        document.insert(5, "int b;\n");
        document.insert(20, "x");
        document.delete(0, 5);

        assertEquals(5, received.size());
        for (int i = 1; i < received.size(); i++) {
            assertTrue(received.get(i).getVersion() > received.get(i - 1).getVersion(),
                    "versions must be strictly monotonic: v" + received.get(i - 1).getVersion()
                            + " -> v" + received.get(i).getVersion());
        }
        assertEquals(document.currentVersion(), received.get(received.size() - 1).getVersion());
    }

    @Test
    void rollbackDoesNotTriggerLexicalUpdate() {
        document.beginBatch();
        document.insert(11, "\n");
        document.insert(0, "import x;\n");
        document.abortBatch();

        assertEquals(0, received.size(), "aborted batch must not publish TokensUpdatedEvent");
        assertEquals(3, document.currentVersion(),
                "mutations in an aborted batch are applied silently (no event fired)");
        assertTrue(document.getText().contains("import x;"));
    }

    @Test
    void disposeIsIdempotentAndStopsDelivery() {
        document.insert(11, "\n");
        assertEquals(1, received.size());

        bridge.dispose();
        bridge.dispose();

        document.insert(0, "// x\n");
        assertEquals(1, received.size(), "no events after dispose, even on double dispose");
    }
}
