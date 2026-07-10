package com.eyecode.project;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.project.ProjectIndexer;
import com.eyecode.editor.v2.project.ProjectSymbolIndex;
import com.eyecode.eventbus.EventBus;
import com.eyecode.eventbus.events.ProjectRefreshEvent;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ProjectRefreshServiceTest {

    @TempDir
    Path tempDir;

    private EventBus eventBus;
    private ProjectSymbolIndex index;
    private ProjectIndexer indexer;
    private ProjectRefreshService service;

    @BeforeEach
    void setUp() {
        eventBus = new EventBus();
        index = new ProjectSymbolIndex();
        indexer = new ProjectIndexer(tempDir);
        service = new ProjectRefreshService(eventBus, indexer, index);
        service.start();
    }

    private Path writeFile(String name, String content) throws IOException {
        Path file = tempDir.resolve(name + ".java");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void fileCreatedEventIndexesFile() throws Exception {
        Path file = writeFile("Created", "class Created { int x; }");

        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_CREATED, file));

        assertFalse(index.getForFile(file).isEmpty());
        assertTrue(index.getForFile(file).stream().anyMatch(s -> "Created".equals(s.getName())));
    }

    @Test
    void fileModifiedEventUpdatesSymbols() throws Exception {
        Path file = writeFile("Modified", "class Modified { void run() {} }");
        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_CREATED, file));
        assertFalse(index.getForFile(file).isEmpty());

        Files.writeString(file, "class Modified { void run() {} String getName() { return null; } }");
        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_MODIFIED, file));

        List<ProjectSymbol> symbols = index.getForFile(file);
        assertTrue(symbols.stream().anyMatch(s -> "getName".equals(s.getName())));
    }

    @Test
    void fileDeletedEventRemovesSymbols() throws Exception {
        Path file = writeFile("Deleted", "class Deleted { int y; }");
        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_CREATED, file));
        assertFalse(index.getForFile(file).isEmpty());

        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_DELETED, file));

        assertTrue(index.getForFile(file).isEmpty());
    }

    @Test
    void fileRenamedEventRemovesOldAndIndexesNew() throws Exception {
        Path oldFile = writeFile("Old", "class Old { int a; }");
        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_CREATED, oldFile));
        assertFalse(index.getForFile(oldFile).isEmpty());

        Path newFile = tempDir.resolve("New.java");
        Files.move(oldFile, newFile);
        Files.writeString(newFile, "class New { int a; }");

        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_RENAMED, newFile, oldFile));

        assertTrue(index.getForFile(oldFile).isEmpty());
        assertFalse(index.getForFile(newFile).isEmpty());
        assertTrue(index.getForFile(newFile).stream().anyMatch(s -> "New".equals(s.getName())));
    }

    @Test
    void directoryEventsDoNotChangeIndex() throws Exception {
        int before = index.size();
        Path dir = tempDir.resolve("newdir");
        Files.createDirectories(dir);

        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.DIRECTORY_CREATED, dir));
        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.DIRECTORY_DELETED, dir));

        assertEquals(before, index.size());
    }

    @Test
    void treeChangeListenerReceivesEvents() throws Exception {
        AtomicReference<ProjectRefreshEvent> received = new AtomicReference<>();
        service.addTreeChangeListener(received::set);

        Path file = writeFile("Listen", "class Listen {}");
        service.emit(new ProjectRefreshEvent(ProjectRefreshEvent.Kind.FILE_CREATED, file));

        ProjectRefreshEvent event = received.get();
        assertNotNull(event);
        assertEquals(ProjectRefreshEvent.Kind.FILE_CREATED, event.getKind());
        assertEquals(file, event.getPath());
    }

    @Test
    void stopUnsubscribesFromBus() {
        service.stop();
        int before = index.size();

        service.emit(new ProjectRefreshEvent(
                ProjectRefreshEvent.Kind.FILE_CREATED, tempDir.resolve("AfterStop.java")));

        assertEquals(before, index.size());
    }
}
