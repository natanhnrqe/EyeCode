package com.eyecode.workbench.editor;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.EditorActivatedEvent;
import com.eyecode.eventbus.events.FileClosedEvent;
import com.eyecode.eventbus.events.FileOpenedEvent;
import com.eyecode.filesystem.DefaultFileSystemService;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EditorManagerTest {

    private static final class StubView implements EditorView {

        private boolean disposed;

        @Override
        public Object getNativeView() {
            return "stub-view";
        }

        @Override
        public void refreshFromDocument() {
        }

        @Override
        public void dispose() {
            disposed = true;
        }

        boolean isDisposed() {
            return disposed;
        }
    }

    private static final EditorViewFactory STUB_FACTORY = new EditorViewFactory() {
        @Override
        public EditorView create(EditorBuffer buffer) {
            return new StubView();
        }

        @Override
        public boolean supports(Path file) {
            return true;
        }

        @Override
        public String id() {
            return "stub";
        }
    };

    private EditorManager newManager() {
        return new EditorManager(new EventBus(), new DefaultFileSystemService(), STUB_FACTORY);
    }

    @Test
    void openDocumentCriaSessaoAtiva() {
        EditorManager manager = newManager();
        EditorSession session = manager.openDocument(Path.of("demo/A.java"), "class A {}");

        assertNotNull(session);
        assertNotNull(session.getSessionId());
        assertNotNull(session.getDocumentId());
        assertEquals("A.java", session.getDisplayName());
        assertEquals(1, manager.getSessions().size());
        assertSame(session, manager.getCurrentSession());
        assertEquals(SessionState.ACTIVE, session.getState());
    }

    @Test
    void openDocumentLeDoDisco() throws Exception {
        Path file = Files.createTempFile("ec-manager", ".java");
        Files.writeString(file, "class Temp {}");

        EditorManager manager = newManager();
        EditorSession session = manager.openDocument(file);

        assertEquals(file, session.getFile());
        String text = manager.getBuffer(session.getSessionId())
                .orElseThrow().getDocument().getText();
        assertEquals("class Temp {}", text);
    }

    @Test
    void abrirMesmoArquivoReutilizaSessao() {
        EditorManager manager = newManager();
        EditorSession first = manager.openDocument(Path.of("demo/A.java"), "x");
        EditorSession second = manager.openDocument(Path.of("demo/A.java"), "y");

        assertSame(first, second);
        assertEquals(1, manager.getSessions().size());
    }

    @Test
    void openDocumentComArquivoInexistenteLancaErro() {
        EditorManager manager = newManager();
        assertThrows(IllegalStateException.class,
                () -> manager.openDocument(Path.of("nao-existe/arquivo.java")));
    }

    @Test
    void activateSessionMudaAtivoEInativaAnterior() {
        EditorManager manager = newManager();
        EditorSession a = manager.openDocument(Path.of("demo/A.java"), "a");
        EditorSession b = manager.openDocument(Path.of("demo/B.java"), "b");

        assertSame(b, manager.getCurrentSession());
        assertEquals(SessionState.INACTIVE, a.getState());
        assertEquals(SessionState.ACTIVE, b.getState());

        manager.activateSession(a.getSessionId());

        assertSame(a, manager.getCurrentSession());
        assertEquals(SessionState.ACTIVE, a.getState());
        assertEquals(SessionState.INACTIVE, b.getState());
    }

    @Test
    void activationKeepsEachOpenDocumentContentAndDirtyState() {
        EditorManager manager = newManager();
        EditorSession a = manager.openDocument(null, "class A {}");
        EditorBuffer aBuffer = manager.getBuffer(a.getSessionId()).orElseThrow();
        aBuffer.replaceText("class A { int value; }");

        EditorSession b = manager.openDocument(null, "class B {}");
        String aSessionId = a.getSessionId();
        String aDocumentId = a.getDocumentId();

        manager.activateSession(aSessionId);

        assertSame(a, manager.getCurrentSession());
        assertEquals(aSessionId, a.getSessionId());
        assertEquals(aDocumentId, a.getDocumentId());
        assertEquals("class A { int value; }", aBuffer.getDocument().getText());
        assertTrue(aBuffer.getDocument().isDirty());
        assertEquals("class B {}", manager.getBuffer(b.getSessionId()).orElseThrow().getDocument().getText());
        assertFalse(manager.getBuffer(b.getSessionId()).orElseThrow().getDocument().isDirty());
    }

    @Test
    void flushSessionKeepsDocumentAndSessionIdentity() throws Exception {
        Path file = Files.createTempFile("ec-manager-save", ".java");
        Files.writeString(file, "class Saved {}");
        EditorManager manager = newManager();
        EditorSession session = manager.openDocument(file);
        EditorBuffer buffer = manager.getBuffer(session.getSessionId()).orElseThrow();
        String sessionId = session.getSessionId();
        String documentId = session.getDocumentId();

        buffer.replaceText("class Saved { int value; }");
        assertTrue(manager.flushSession(sessionId));

        assertSame(session, manager.getSession(sessionId).orElseThrow());
        assertEquals(documentId, session.getDocumentId());
        assertSame(buffer, manager.getBuffer(sessionId).orElseThrow());
        assertEquals("class Saved { int value; }", buffer.getDocument().getText());
        assertFalse(buffer.getDocument().isDirty());
        assertEquals("class Saved { int value; }", Files.readString(file));
        manager.shutdownAutosave();
    }

    @Test
    void definitionResolutionUsesCurrentDirtyDocumentSnapshot() throws Exception {
        EditorManager manager = newManager();
        EditorSession session = manager.openDocument(Files.createTempFile("ec-live", ".java"), "class Live { void use() {} }");
        EditorBuffer buffer = manager.getBuffer(session.getSessionId()).orElseThrow();
        String source = "class Live { int current; void use() { current = 1; } }";
        buffer.replaceText(source);
        int reference = source.lastIndexOf("current");

        var location = manager.resolveDefinition(session.getSessionId(), reference);

        assertTrue(buffer.getDocument().isDirty());
        assertTrue(location.isPresent());
        assertEquals(source.indexOf("int current"), location.orElseThrow().declarationRange().startOffset());
    }

    @Test
    void closeSessionRemoveSessaoEInvocaViewDispose() {
        EditorManager manager = newManager();
        EditorSession a = manager.openDocument(Path.of("demo/A.java"), "a");
        EditorSession b = manager.openDocument(Path.of("demo/B.java"), "b");

        StubView view = (StubView) manager.getView(b.getSessionId()).orElseThrow();
        manager.closeSession(b.getSessionId());

        assertEquals(1, manager.getSessions().size());
        assertFalse(manager.getSession(b.getSessionId()).isPresent());
        assertEquals(SessionState.DISPOSED, b.getState());
        assertTrue(view.isDisposed());
        assertSame(a, manager.getCurrentSession());
    }

    @Test
    void closeSessionComAtivaReativaVizinha() {
        EditorManager manager = newManager();
        EditorSession a = manager.openDocument(Path.of("demo/A.java"), "a");
        EditorSession b = manager.openDocument(Path.of("demo/B.java"), "b");
        EditorSession c = manager.openDocument(Path.of("demo/C.java"), "c");

        manager.closeSession(b.getSessionId());
        assertSame(c, manager.getCurrentSession());

        manager.closeSession(c.getSessionId());
        assertSame(a, manager.getCurrentSession());
    }

    @Test
    void closeSessionEIdempotente() {
        EditorManager manager = newManager();
        EditorSession a = manager.openDocument(Path.of("demo/A.java"), "a");

        manager.closeSession(a.getSessionId());
        manager.closeSession(a.getSessionId());

        assertEquals(0, manager.getSessions().size());
    }

    @Test
    void closeAllSessionsClearsActiveSessionForWorkspaceReplacement() {
        EditorManager manager = newManager();
        EditorSession a = manager.openDocument(Path.of("demo/A.java"), "class A {}");
        EditorSession b = manager.openDocument(Path.of("demo/B.java"), "class B {}");

        manager.closeAllSessions();

        assertTrue(manager.getSessions().isEmpty());
        assertNull(manager.getCurrentSession());
        assertFalse(manager.getSession(a.getSessionId()).isPresent());
        assertFalse(manager.getSession(b.getSessionId()).isPresent());
        assertEquals(SessionState.DISPOSED, a.getState());
        assertEquals(SessionState.DISPOSED, b.getState());
    }

    @Test
    void workspaceStateListenersDisparam() {
        EditorManager manager = newManager();
        AtomicInteger changes = new AtomicInteger();
        AtomicReference<EditorSession> activeRef = new AtomicReference<>();
        manager.getWorkspaceState().addChangeListener(changes::incrementAndGet);
        manager.getWorkspaceState().addActiveSessionListener(activeRef::set);

        EditorSession a = manager.openDocument(Path.of("demo/A.java"), "a");
        manager.openDocument(Path.of("demo/B.java"), "b");

        assertTrue(changes.get() >= 2);
        assertSame(activeRef.get(), manager.getCurrentSession());
    }

    @Test
    void workspaceStateFindSessionByFile() {
        EditorManager manager = newManager();
        Path file = Path.of("demo/A.java");
        manager.openDocument(file, "a");

        assertTrue(manager.getWorkspaceState().findSessionByFile(file).isPresent());
        assertFalse(manager.getWorkspaceState().findSessionByFile(Path.of("outro.java")).isPresent());
    }

    @Test
    void eventsSaoPublicados() {
        EventBus bus = new EventBus();
        EditorManager manager = new EditorManager(bus, new DefaultFileSystemService(), STUB_FACTORY);

        List<Path> opened = new ArrayList<>();
        List<Path> closed = new ArrayList<>();
        List<EditorSession> activated = new ArrayList<>();
        bus.subscribe(FileOpenedEvent.class, e -> opened.add(e.getFile().toPath()));
        bus.subscribe(FileClosedEvent.class, e -> closed.add(e.getFile().toPath()));
        bus.subscribe(EditorActivatedEvent.class, e -> activated.add(e.getSession()));

        Path file = Path.of("demo/A.java");
        EditorSession session = manager.openDocument(file, "a");
        manager.closeSession(session.getSessionId());

        assertEquals(List.of(file), opened);
        assertEquals(List.of(file), closed);
        assertFalse(activated.isEmpty());
        assertSame(session, activated.get(0));
    }

    @Test
    void historyRegistraFechamento() {
        EditorManager manager = newManager();
        Path file = Path.of("demo/A.java");
        EditorSession session = manager.openDocument(file, "a");

        manager.closeSession(session.getSessionId());

        assertEquals(1, manager.getHistory().recentlyClosedSize());
        assertEquals(file, manager.getHistory().peekRecentlyClosed().file());
        assertEquals(file, manager.getHistory().popRecentlyClosed().file());
        assertEquals(0, manager.getHistory().recentlyClosedSize());
    }

    @Test
    void selectionServiceSnapshotsCaret() {
        EditorManager manager = newManager();
        EditorSession session = manager.openDocument(Path.of("demo/A.java"), "class A {}");
        EditorSelectionService service = manager.getSelectionService();

        EditorPosition caret = new EditorPosition(0, 3);
        service.setCaret(session, caret);
        assertEquals(caret, session.getCaretState());

        EditorViewport viewport = service.captureViewport(session);
        assertEquals(caret, viewport.caret());

        EditorPosition restored = new EditorPosition(1, 7);
        service.restoreViewport(session, new EditorViewport(session.getFile(), restored, EditorScroll.zero()));
        assertEquals(restored, session.getCaretState());
    }

    @Test
    void selectionServiceGetSelectionAndScroll() {
        EditorManager manager = newManager();
        EditorSession session = manager.openDocument(Path.of("demo/A.java"), "class A {}");
        EditorSelectionService service = manager.getSelectionService();

        EditorSelection selection = new EditorSelection(new EditorPosition(0, 0), new EditorPosition(0, 5));
        service.setSelection(session, selection);
        assertEquals(selection, service.getSelectionState(session));

        EditorScroll scroll = new EditorScroll(10.0, 20.0);
        service.setScrollState(session, scroll);
        assertEquals(scroll, service.getScrollState(session));
    }
}
