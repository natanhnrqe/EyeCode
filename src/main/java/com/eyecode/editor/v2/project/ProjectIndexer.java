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

        indexSourceDir(projectRoot.resolve("src/main/java"), index);
        indexSourceDir(projectRoot.resolve("src/test/java"), index);
    }

    private void indexSourceDir(Path dir, ProjectSymbolIndex index) {
        if (!Files.isDirectory(dir)) return;

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> indexFile(file, index));
        } catch (IOException ignored) {
        }
    }

    private void indexFile(Path file, ProjectSymbolIndex index) {
        try {
            String source = Files.readString(file);
            JavaLexer lexer = new JavaLexer();
            JavaTokenStream stream = new JavaTokenStream(lexer.tokenize(source));
            JavaParser parser = new JavaParser(stream);
            JavaFileModel model = parser.parse();

            SymbolBuilder symbolBuilder = new SymbolBuilder();
            List<ProjectSymbol> symbols = symbolBuilder.build(model, file);

            for (ProjectSymbol symbol : symbols) {
                index.add(symbol);
            }
        } catch (Exception ignored) {
        }
    }
}
