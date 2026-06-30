package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaNioSymbols {

    private JavaNioSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                CompletionItem.builder("Path", "Path", CompletionItemKind.INTERFACE)
                        .detail("java.nio.file.Path")
                        .owner("java.nio.file")
                        .category("Interface")
                        .documentation("Represents a path in the filesystem, replacing java.io.File for modern code.")
                        .build(),

                CompletionItem.builder("Paths", "Paths", CompletionItemKind.CLASS)
                        .detail("java.nio.file.Paths")
                        .owner("java.nio.file")
                        .category("Class")
                        .documentation("Provides static methods to create Path instances from strings or URIs.")
                        .example("Path path = Paths.get(\"file.txt\");")
                        .build(),

                CompletionItem.builder("Files", "Files", CompletionItemKind.CLASS)
                        .detail("java.nio.file.Files")
                        .owner("java.nio.file")
                        .category("Class")
                        .documentation("Provides static methods for file operations: read, write, copy, delete, and more.")
                        .build(),

                CompletionItem.builder("ByteBuffer", "ByteBuffer", CompletionItemKind.CLASS)
                        .detail("java.nio.ByteBuffer")
                        .owner("java.nio")
                        .category("Class")
                        .documentation("A container for a sequence of bytes, used for NIO channel-based I/O.")
                        .build()
        );
    }
}
