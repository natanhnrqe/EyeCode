package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.language.java.lexer.JavaLexer;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SymbolBuilder;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

public final class ProjectIndexer {

    private final Path projectRoot;

    public ProjectIndexer(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void index(ProjectSymbolIndex index) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) return;

        index.clear();

        Path mainSrc = projectRoot.resolve("src/main/java");
        Path testSrc = projectRoot.resolve("src/test/java");

        boolean hasSrcDir = Files.isDirectory(mainSrc) || Files.isDirectory(testSrc);

        if (Files.isDirectory(mainSrc)) {
            indexSourceDir(mainSrc, index);
        }
        if (Files.isDirectory(testSrc)) {
            indexSourceDir(testSrc, index);
        }

        // Fallback: scan entire project root for .java files when no src/ dirs exist
        if (!hasSrcDir) {
            indexSourceDir(projectRoot, index);
        }
    }

    private void indexSourceDir(Path dir, ProjectSymbolIndex index) {
        if (!Files.isDirectory(dir)) return;

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> indexFile(file, index));
        } catch (IOException ignored) {
        }
    }

    public void indexFile(Path file, ProjectSymbolIndex index) {
        indexFile(file, index, readSource(file));
    }

    private void indexFile(Path file, ProjectSymbolIndex index, String source) {
        if (file == null || index == null) return;
        if (source == null) return;
        List<ProjectSymbol> symbols = parseSymbols(file, source);
        index.replaceFile(file, symbols);
    }

    public void removeFile(Path file, ProjectSymbolIndex index) {
        if (file == null || index == null) return;
        index.removeFile(file);
    }

    public void updateFile(Path file, ProjectSymbolIndex index) {
        if (file == null || index == null) return;
        updateFile(file, index, readSource(file));
    }

    public void updateFile(Path file, ProjectSymbolIndex index, String source) {
        if (file == null || index == null) return;
        if (!isIndexedSource(file)) return;
        String effectiveSource = source != null ? source : readSource(file);
        List<ProjectSymbol> symbols = parseSymbols(file, effectiveSource);
        index.replaceFile(file, symbols);
    }

    public static List<ProjectSymbol> parseSymbols(Path file, String source) {
        if (source == null) return List.of();
        try {
            JavaLexer lexer = new JavaLexer();
            JavaTokenStream stream = new JavaTokenStream(lexer.tokenize(source));
            JavaParser parser = new JavaParser(stream);
            JavaFileModel model = parser.parse();

            SymbolBuilder symbolBuilder = new SymbolBuilder();
            List<ProjectSymbol> symbols = symbolBuilder.build(model, file);
            return new ArrayList<>(symbols);
        } catch (Exception e) {
            System.err.println("[ProjectIndexer] Failed to parse " + file + ": " + e.getMessage());
            return List.of();
        }
    }

    private boolean isIndexedSource(Path file) {
        if (file == null) return false;
        String name = file.getFileName().toString();
        return name.endsWith(".java");
    }

    private String readSource(Path file) {
        if (file == null || !Files.isRegularFile(file)) return null;
        try {
            return Files.readString(file);
        } catch (IOException ignored) {
            return null;
        }
    }
}
