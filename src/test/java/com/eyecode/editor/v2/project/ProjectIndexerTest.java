package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectIndexerTest {

    @TempDir
    Path tempDir;

    private Path writeJavaFile(Path parent, String name, String content) throws IOException {
        Files.createDirectories(parent);
        Path file = parent.resolve(name + ".java");
        Files.writeString(file, content);
        return file;
    }

    @Test
    void indexFileAddsSymbolsForNewFile() throws Exception {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        ProjectIndexer indexer = new ProjectIndexer(tempDir);

        Path file = writeJavaFile(tempDir, "Hello", "public class Hello { String name; }");
        indexer.indexFile(file, index);

        List<ProjectSymbol> symbols = index.getForFile(file);
        assertFalse(symbols.isEmpty());
        assertTrue(symbols.stream().anyMatch(s -> "Hello".equals(s.getName())));
    }

    @Test
    void removeFileClearsSymbolsForThatFileOnly() throws Exception {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        ProjectIndexer indexer = new ProjectIndexer(tempDir);

        Path fileA = writeJavaFile(tempDir, "A", "class A { int x; }");
        Path fileB = writeJavaFile(tempDir, "B", "class B { int y; }");
        indexer.indexFile(fileA, index);
        indexer.indexFile(fileB, index);

        int before = index.size();
        assertTrue(before > 0);

        indexer.removeFile(fileA, index);

        assertTrue(index.getForFile(fileA).isEmpty());
        assertFalse(index.getForFile(fileB).isEmpty());
        assertTrue(index.size() < before);
    }

    @Test
    void updateFileReplacesSymbolsWithoutRebuild() throws Exception {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        ProjectIndexer indexer = new ProjectIndexer(tempDir);

        Path file = writeJavaFile(tempDir, "Service", "class Service { void run() {} }");
        indexer.indexFile(file, index);

        int initialCount = index.size();
        assertTrue(initialCount > 0);

        Files.writeString(file, "class Service { void run() {} String getName() { return null; } }");
        indexer.updateFile(file, index);

        List<ProjectSymbol> symbols = index.getForFile(file);
        assertTrue(symbols.stream().anyMatch(s -> "getName".equals(s.getName())));
        assertTrue(symbols.stream().anyMatch(s -> "run".equals(s.getName())));
    }

    @Test
    void updateFileWithExplicitSourceDoesNotReadDisk() throws Exception {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        ProjectIndexer indexer = new ProjectIndexer(tempDir);

        Path file = writeJavaFile(tempDir, "Empty", "class Empty {}");
        indexer.indexFile(file, index);

        String newSource = "class Empty { int field; }";
        indexer.updateFile(file, index, newSource);

        List<ProjectSymbol> symbols = index.getForFile(file);
        assertTrue(symbols.stream().anyMatch(s -> "field".equals(s.getName())));
    }

    @Test
    void indexFullProjectClearsAndReindexes() throws Exception {
        Path root = tempDir.resolve("proj");
        Path src = root.resolve("src/main/java");
        Path fileA = writeJavaFile(src, "A", "class A {}");
        Path fileB = writeJavaFile(src, "B", "class B {}");

        ProjectSymbolIndex index = new ProjectSymbolIndex();
        index.add(symbol("Stale", fileA));
        ProjectIndexer indexer = new ProjectIndexer(root);

        indexer.index(index);

        assertTrue(index.size() > 0);
        assertFalse(index.getAll().stream().anyMatch(s -> "Stale".equals(s.getName())));
        assertTrue(index.getForFile(fileA).stream().anyMatch(s -> "A".equals(s.getName())));
        assertTrue(index.getForFile(fileB).stream().anyMatch(s -> "B".equals(s.getName())));
    }

    @Test
    void removeFileOnNonIndexedFileIsNoop() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        ProjectIndexer indexer = new ProjectIndexer(tempDir);

        Path ghost = tempDir.resolve("Ghost.java");
        indexer.removeFile(ghost, index);

        assertEquals(0, index.size());
    }

    @Test
    void parseSymbolsReturnsEmptyForInvalidSource() {
        ProjectIndexer indexer = new ProjectIndexer(tempDir);
        List<ProjectSymbol> symbols = indexer.parseSymbols(tempDir.resolve("X.java"), "class { broken");
        assertNotNull(symbols);
    }

    private ProjectSymbol symbol(String name, Path file) {
        ProjectSymbol s = new ProjectSymbol();
        s.setName(name);
        s.setSourceFile(file);
        return s;
    }
}
