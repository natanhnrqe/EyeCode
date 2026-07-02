package com.eyecode.editor.v2.project;

import com.eyecode.editor.v2.completion.CompletionItem;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;

public final class ProjectIndexer {

    private final Path projectRoot;

    public ProjectIndexer(Path projectRoot) {
        this.projectRoot = projectRoot;
    }

    public void index(ProjectSymbolIndex index) {
        if (projectRoot == null || !Files.isDirectory(projectRoot)) return;

        index.clear();
        JavaFileParser parser = new JavaFileParser();

        indexSourceDir(projectRoot.resolve("src/main/java"), parser, index);
        indexSourceDir(projectRoot.resolve("src/test/java"), parser, index);
    }

    private void indexSourceDir(Path dir, JavaFileParser parser, ProjectSymbolIndex index) {
        if (!Files.isDirectory(dir)) return;

        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(p -> p.toString().endsWith(".java"))
                .forEach(file -> indexFile(file, parser, index));
        } catch (IOException ignored) {
        }
    }

    private void indexFile(Path file, JavaFileParser parser, ProjectSymbolIndex index) {
        try {
            String source = Files.readString(file);
            for (CompletionItem item : parser.parse(source)) {
                index.add(item);
            }
        } catch (Exception ignored) {
        }
    }
}
