package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ProjectSymbolIndexTest {

    private static ProjectSymbol symbol(String name, Path file) {
        ProjectSymbol s = new ProjectSymbol();
        s.setName(name);
        s.setSourceFile(file);
        return s;
    }

    @Test
    void addAndRetrieveByFile() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        Path file = Path.of("A.java");

        index.add(symbol("Foo", file));
        index.add(symbol("Bar", file));

        List<ProjectSymbol> forFile = index.getForFile(file);
        assertEquals(2, forFile.size());
        assertEquals(2, index.size());
    }

    @Test
    void removeFileDropsOnlyThatFileSymbols() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        Path fileA = Path.of("A.java");
        Path fileB = Path.of("B.java");

        index.add(symbol("A1", fileA));
        index.add(symbol("A2", fileA));
        index.add(symbol("B1", fileB));

        index.removeFile(fileA);

        assertTrue(index.getForFile(fileA).isEmpty());
        assertEquals(1, index.getForFile(fileB).size());
        assertEquals(1, index.size());
    }

    @Test
    void replaceFileSwapsSymbols() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        Path file = Path.of("C.java");

        index.add(symbol("Old", file));
        index.replaceFile(file, List.of(symbol("New1", file), symbol("New2", file)));

        List<ProjectSymbol> forFile = index.getForFile(file);
        assertEquals(2, forFile.size());
        assertTrue(forFile.stream().anyMatch(s -> "New1".equals(s.getName())));
        assertTrue(forFile.stream().anyMatch(s -> "New2".equals(s.getName())));
        assertFalse(forFile.stream().anyMatch(s -> "Old".equals(s.getName())));
    }

    @Test
    void clearWipesEverything() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        Path file = Path.of("D.java");
        index.add(symbol("X", file));

        index.clear();

        assertEquals(0, index.size());
        assertTrue(index.getForFile(file).isEmpty());
    }

    @Test
    void removeFileOnUnknownPathIsNoop() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        index.removeFile(Path.of("ghost.java"));
        assertEquals(0, index.size());
    }

    @Test
    void addNullSymbolIsIgnored() {
        ProjectSymbolIndex index = new ProjectSymbolIndex();
        index.add(null);
        index.add(symbol(null, Path.of("E.java")));
        assertEquals(0, index.size());
    }
}
