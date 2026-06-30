package com.eyecode.editor.v2.completion.database;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;

import java.util.List;

public final class JavaIoSymbols {

    private JavaIoSymbols() {}

    public static List<CompletionItem> getAll() {
        return List.of(
                CompletionItem.builder("File", "File", CompletionItemKind.CLASS)
                        .detail("java.io.File")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Represents a file or directory path in the filesystem.")
                        .build(),

                CompletionItem.builder("InputStream", "InputStream", CompletionItemKind.CLASS)
                        .detail("java.io.InputStream")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Abstract base class for byte input streams.")
                        .build(),

                CompletionItem.builder("OutputStream", "OutputStream", CompletionItemKind.CLASS)
                        .detail("java.io.OutputStream")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Abstract base class for byte output streams.")
                        .build(),

                CompletionItem.builder("Reader", "Reader", CompletionItemKind.CLASS)
                        .detail("java.io.Reader")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Abstract base class for reading character streams.")
                        .build(),

                CompletionItem.builder("Writer", "Writer", CompletionItemKind.CLASS)
                        .detail("java.io.Writer")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Abstract base class for writing character streams.")
                        .build(),

                CompletionItem.builder("BufferedReader", "BufferedReader", CompletionItemKind.CLASS)
                        .detail("java.io.BufferedReader")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Reads text from a character input stream, buffering characters for efficient reading.")
                        .example("BufferedReader br = new BufferedReader(new FileReader(\"file.txt\"));\nString line = br.readLine();")
                        .build(),

                CompletionItem.builder("BufferedWriter", "BufferedWriter", CompletionItemKind.CLASS)
                        .detail("java.io.BufferedWriter")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Writes text to a character output stream, buffering characters for efficient writing.")
                        .build(),

                CompletionItem.builder("PrintStream", "PrintStream", CompletionItemKind.CLASS)
                        .detail("java.io.PrintStream")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Adds functionality to an output stream for printing representations of various data values.")
                        .build(),

                CompletionItem.builder("Serializable", "Serializable", CompletionItemKind.INTERFACE)
                        .detail("java.io.Serializable")
                        .owner("java.io")
                        .category("Interface")
                        .documentation("Marker interface indicating a class can be serialized to a byte stream.")
                        .build()
        );
    }
}
