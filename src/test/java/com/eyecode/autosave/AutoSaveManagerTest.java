package com.eyecode.autosave;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.filesystem.FileSystemService;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AutoSaveManagerTest {

    private static final long DELAY_MS = 60L;

    private ScheduledExecutorService executor;

    private ScheduledExecutorService newExecutor() {
        executor = Executors.newSingleThreadScheduledExecutor();
        return executor;
    }

    @AfterEach
    void tearDown() {
        if (executor != null) {
            executor.shutdownNow();
        }
    }

    @Test
    void savesAfterDebounceDelay() throws Exception {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        Path path = Path.of("Main.java");
        EditorDocument doc = new EditorDocument(path, "original");
        manager.register(doc);

        doc.insert(0, "x");

        assertTrue(waitUntil(() -> fs.writeCount.get() >= 1));
        assertEquals("xoriginal", fs.files.get(path));
        assertFalse(doc.isDirty());
        manager.shutdown();
    }

    @Test
    void rapidChangesProduceSingleDebouncedSave() throws Exception {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        Path path = Path.of("A.txt");
        EditorDocument doc = new EditorDocument(path, "base");
        manager.register(doc);

        doc.insert(0, "a");
        doc.insert(1, "b");
        doc.insert(2, "c");

        assertTrue(waitUntil(() -> fs.writeCount.get() >= 1));
        Thread.sleep(DELAY_MS * 3);

        assertEquals(1, fs.writeCount.get());
        assertEquals("abcbase", fs.files.get(path));
        assertFalse(doc.isDirty());
        manager.shutdown();
    }

    @Test
    void saveNowPersistsImmediatelyAndCancelsPending() throws Exception {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        Path path = Path.of("B.txt");
        EditorDocument doc = new EditorDocument(path, "data");
        manager.register(doc);

        doc.insert(0, "x");
        manager.saveNow(doc);

        assertEquals(1, fs.writeCount.get());
        assertEquals("xdata", fs.files.get(path));
        assertFalse(doc.isDirty());

        Thread.sleep(DELAY_MS * 3);
        assertEquals(1, fs.writeCount.get());
        manager.shutdown();
    }

    @Test
    void saveAllPersistsEveryDirtyDocument() {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        EditorDocument doc1 = new EditorDocument(Path.of("one.txt"), "one");
        EditorDocument doc2 = new EditorDocument(Path.of("two.txt"), "two");
        manager.register(doc1);
        manager.register(doc2);

        doc1.insert(0, "1");
        doc2.insert(0, "2");

        manager.saveAll();

        assertEquals(2, fs.writeCount.get());
        assertEquals("1one", fs.files.get(Path.of("one.txt")));
        assertEquals("2two", fs.files.get(Path.of("two.txt")));
        assertFalse(doc1.isDirty());
        assertFalse(doc2.isDirty());
        manager.shutdown();
    }

    @Test
    void unregisterStopsTrackingDocument() throws Exception {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        Path path = Path.of("C.txt");
        EditorDocument doc = new EditorDocument(path, "c");
        manager.register(doc);
        manager.unregister(doc);

        doc.insert(0, "x");
        Thread.sleep(DELAY_MS * 3);

        assertEquals(0, fs.writeCount.get());
        assertTrue(doc.isDirty());
        manager.shutdown();
    }

    @Test
    void untitledDocumentWithoutPathIsNotSaved() throws Exception {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        EditorDocument doc = new EditorDocument();
        manager.register(doc);

        doc.insert(0, "x");
        Thread.sleep(DELAY_MS * 3);

        assertEquals(0, fs.writeCount.get());
        assertTrue(doc.isDirty());
        manager.shutdown();
    }

    @Test
    void cleanDocumentIsNotRewritten() throws Exception {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        Path path = Path.of("D.txt");
        EditorDocument doc = new EditorDocument(path, "clean");
        manager.register(doc);

        manager.saveNow(doc);
        assertEquals(0, fs.writeCount.get());
        manager.shutdown();
    }

    @Test
    void saveListenerReceivesEvent() throws Exception {
        FakeFileSystem fs = new FakeFileSystem();
        AutoSaveManager manager = new AutoSaveManager(fs, DELAY_MS, newExecutor());

        CountDownLatch latch = new CountDownLatch(1);
        manager.addSaveListener(event -> {
            if (event.succeeded()) latch.countDown();
        });

        Path path = Path.of("E.txt");
        EditorDocument doc = new EditorDocument(path, "e");
        manager.register(doc);

        doc.insert(0, "x");

        assertTrue(latch.await(2, TimeUnit.SECONDS));
        manager.shutdown();
    }

    private static boolean waitUntil(java.util.function.BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 2000;
        while (System.currentTimeMillis() < deadline) {
            if (condition.getAsBoolean()) return true;
            Thread.sleep(10);
        }
        return condition.getAsBoolean();
    }

    private static final class FakeFileSystem implements FileSystemService {
        final Map<Path, String> files = new ConcurrentHashMap<>();
        final AtomicInteger writeCount = new AtomicInteger();

        @Override
        public String readFile(Path path) {
            return files.getOrDefault(path, "");
        }

        @Override
        public void writeFile(Path path, String content) {
            files.put(path, content);
            writeCount.incrementAndGet();
        }

        @Override
        public boolean exists(Path path) {
            return files.containsKey(path);
        }

        @Override
        public void createDirectories(Path path) {
        }

        @Override
        public List<Path> listFiles(Path directory) {
            return List.of();
        }

        @Override
        public List<Path> findFiles(Path root, String globPattern) {
            return List.of();
        }

        @Override
        public void copyResource(InputStream source, Path target) {
        }
    }
}
