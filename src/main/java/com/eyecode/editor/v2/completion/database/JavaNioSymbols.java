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
                        .build(),

                // -- Path Methods --
                CompletionItem.builder("getFileName", "getFileName", CompletionItemKind.METHOD)
                        .detail("Path.getFileName")
                        .signature("Path.getFileName()")
                        .returnType("Path")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Returns the name of the file or directory as a Path object (the last element).")
                        .build(),

                CompletionItem.builder("getParent", "getParent", CompletionItemKind.METHOD)
                        .detail("Path.getParent")
                        .signature("Path.getParent()")
                        .returnType("Path")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Returns the parent path, or null if this path has no parent.")
                        .build(),

                CompletionItem.builder("getNameCount", "getNameCount", CompletionItemKind.METHOD)
                        .detail("Path.getNameCount")
                        .signature("Path.getNameCount()")
                        .returnType("int")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Returns the number of name elements in the path.")
                        .build(),

                CompletionItem.builder("getName", "getName", CompletionItemKind.METHOD)
                        .detail("Path.getName")
                        .signature("Path.getName(int index)")
                        .returnType("Path")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Returns a name element at the given index (0-based).")
                        .build(),

                CompletionItem.builder("normalize", "normalize", CompletionItemKind.METHOD)
                        .detail("Path.normalize")
                        .signature("Path.normalize()")
                        .returnType("Path")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Returns a path that eliminates redundant elements like . and ..")
                        .build(),

                CompletionItem.builder("resolve", "resolve", CompletionItemKind.METHOD)
                        .detail("Path.resolve")
                        .signature("Path.resolve(String other)")
                        .returnType("Path")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Resolves the given path against this path, combining them.")
                        .build(),

                CompletionItem.builder("relativize", "relativize", CompletionItemKind.METHOD)
                        .detail("Path.relativize")
                        .signature("Path.relativize(Path other)")
                        .returnType("Path")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Constructs a relative path from this path to the given path.")
                        .build(),

                CompletionItem.builder("toAbsolutePath", "toAbsolutePath", CompletionItemKind.METHOD)
                        .detail("Path.toAbsolutePath")
                        .signature("Path.toAbsolutePath()")
                        .returnType("Path")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Returns the absolute path representation of this path.")
                        .build(),

                CompletionItem.builder("toFile", "toFile", CompletionItemKind.METHOD)
                        .detail("Path.toFile")
                        .signature("Path.toFile()")
                        .returnType("File")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Converts this path to a java.io.File object.")
                        .build(),

                CompletionItem.builder("toUri", "toUri", CompletionItemKind.METHOD)
                        .detail("Path.toUri")
                        .signature("Path.toUri()")
                        .returnType("URI")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Converts this path to a file: URI representing the path.")
                        .build(),

                CompletionItem.builder("isAbsolute", "isAbsolute", CompletionItemKind.METHOD)
                        .detail("Path.isAbsolute")
                        .signature("Path.isAbsolute()")
                        .returnType("boolean")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Tests if this path is absolute (has a root component).")
                        .build(),

                CompletionItem.builder("startsWith", "startsWith", CompletionItemKind.METHOD)
                        .detail("Path.startsWith")
                        .signature("Path.startsWith(String other)")
                        .returnType("boolean")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Tests if this path starts with the given path string.")
                        .build(),

                CompletionItem.builder("endsWith", "endsWith", CompletionItemKind.METHOD)
                        .detail("Path.endsWith")
                        .signature("Path.endsWith(String other)")
                        .returnType("boolean")
                        .owner("java.nio.file.Path")
                        .category("Method")
                        .documentation("Tests if this path ends with the given path string.")
                        .build(),

                // -- Paths Methods --
                CompletionItem.builder("get", "get", CompletionItemKind.METHOD)
                        .detail("Paths.get")
                        .signature("Paths.get(String first, String... more)")
                        .returnType("Path")
                        .owner("java.nio.file.Paths")
                        .category("Method")
                        .documentation("Converts a path string (or strings) to a Path instance.")
                        .build(),

                // -- Files Methods --
                CompletionItem.builder("readAllBytes", "readAllBytes", CompletionItemKind.METHOD)
                        .detail("Files.readAllBytes")
                        .signature("Files.readAllBytes(Path path)")
                        .returnType("byte[]")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Reads all bytes from a file into a byte array.")
                        .build(),

                CompletionItem.builder("readAllLines", "readAllLines", CompletionItemKind.METHOD)
                        .detail("Files.readAllLines")
                        .signature("Files.readAllLines(Path path)")
                        .returnType("List<String>")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Reads all lines from a file into a List of strings.")
                        .build(),

                CompletionItem.builder("write", "write", CompletionItemKind.METHOD)
                        .detail("Files.write")
                        .signature("Files.write(Path path, byte[] bytes)")
                        .returnType("Path")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Writes bytes to a file, creating or overwriting the file.")
                        .build(),

                CompletionItem.builder("copy", "copy", CompletionItemKind.METHOD)
                        .detail("Files.copy")
                        .signature("Files.copy(Path source, Path target, CopyOption... options)")
                        .returnType("Path")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Copies a file from source to target with optional copy options.")
                        .build(),

                CompletionItem.builder("move", "move", CompletionItemKind.METHOD)
                        .detail("Files.move")
                        .signature("Files.move(Path source, Path target, CopyOption... options)")
                        .returnType("Path")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Moves or renames a file from source to target.")
                        .build(),

                CompletionItem.builder("deleteIfExists", "deleteIfExists", CompletionItemKind.METHOD)
                        .detail("Files.deleteIfExists")
                        .signature("Files.deleteIfExists(Path path)")
                        .returnType("boolean")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Deletes a file if it exists, silently returning false if it does not.")
                        .build(),

                CompletionItem.builder("exists", "exists", CompletionItemKind.METHOD)
                        .detail("Files.exists")
                        .signature("Files.exists(Path path, LinkOption... options)")
                        .returnType("boolean")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Tests if a file exists on the filesystem.")
                        .build(),

                CompletionItem.builder("notExists", "notExists", CompletionItemKind.METHOD)
                        .detail("Files.notExists")
                        .signature("Files.notExists(Path path, LinkOption... options)")
                        .returnType("boolean")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Tests if the file does not exist on the filesystem.")
                        .build(),

                CompletionItem.builder("isDirectory", "isDirectory", CompletionItemKind.METHOD)
                        .detail("Files.isDirectory")
                        .signature("Files.isDirectory(Path path, LinkOption... options)")
                        .returnType("boolean")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Tests if the path refers to a directory.")
                        .build(),

                CompletionItem.builder("isRegularFile", "isRegularFile", CompletionItemKind.METHOD)
                        .detail("Files.isRegularFile")
                        .signature("Files.isRegularFile(Path path, LinkOption... options)")
                        .returnType("boolean")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Tests if the path refers to a regular file.")
                        .build(),

                CompletionItem.builder("createFile", "createFile", CompletionItemKind.METHOD)
                        .detail("Files.createFile")
                        .signature("Files.createFile(Path path, FileAttribute<?>... attrs)")
                        .returnType("Path")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Creates a new empty file at the given path.")
                        .build(),

                CompletionItem.builder("createDirectory", "createDirectory", CompletionItemKind.METHOD)
                        .detail("Files.createDirectory")
                        .signature("Files.createDirectory(Path dir, FileAttribute<?>... attrs)")
                        .returnType("Path")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Creates a single directory at the given path.")
                        .build(),

                CompletionItem.builder("createDirectories", "createDirectories", CompletionItemKind.METHOD)
                        .detail("Files.createDirectories")
                        .signature("Files.createDirectories(Path dir, FileAttribute<?>... attrs)")
                        .returnType("Path")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Creates directories recursively, creating parent directories as needed.")
                        .build(),

                CompletionItem.builder("newBufferedReader", "newBufferedReader", CompletionItemKind.METHOD)
                        .detail("Files.newBufferedReader")
                        .signature("Files.newBufferedReader(Path path)")
                        .returnType("BufferedReader")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Opens a file for reading, returning a BufferedReader for efficient text reading.")
                        .build(),

                CompletionItem.builder("newBufferedWriter", "newBufferedWriter", CompletionItemKind.METHOD)
                        .detail("Files.newBufferedWriter")
                        .signature("Files.newBufferedWriter(Path path)")
                        .returnType("BufferedWriter")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Opens or creates a file for writing, returning a BufferedWriter.")
                        .build(),

                CompletionItem.builder("newInputStream", "newInputStream", CompletionItemKind.METHOD)
                        .detail("Files.newInputStream")
                        .signature("Files.newInputStream(Path path, OpenOption... options)")
                        .returnType("InputStream")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Opens a file for reading, returning an InputStream.")
                        .build(),

                CompletionItem.builder("newOutputStream", "newOutputStream", CompletionItemKind.METHOD)
                        .detail("Files.newOutputStream")
                        .signature("Files.newOutputStream(Path path, OpenOption... options)")
                        .returnType("OutputStream")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Opens or creates a file for writing, returning an OutputStream.")
                        .build(),

                CompletionItem.builder("lines", "lines", CompletionItemKind.METHOD)
                        .detail("Files.lines")
                        .signature("Files.lines(Path path)")
                        .returnType("Stream<String>")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Reads all lines from a file as a Stream (lazily populated).")
                        .build(),

                CompletionItem.builder("list", "list", CompletionItemKind.METHOD)
                        .detail("Files.list")
                        .signature("Files.list(Path dir)")
                        .returnType("Stream<Path>")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Returns a lazy Stream of entries in a directory.")
                        .build(),

                CompletionItem.builder("walk", "walk", CompletionItemKind.METHOD)
                        .detail("Files.walk")
                        .signature("Files.walk(Path start, int maxDepth, FileVisitOption... options)")
                        .returnType("Stream<Path>")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Walks the file tree recursively, returning a Stream of Path entries.")
                        .build(),

                CompletionItem.builder("find", "find", CompletionItemKind.METHOD)
                        .detail("Files.find")
                        .signature("Files.find(Path start, int maxDepth, BiPredicate<Path, BasicFileAttributes> matcher, FileVisitOption... options)")
                        .returnType("Stream<Path>")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Walks the file tree and returns entries matching the given predicate.")
                        .build(),

                CompletionItem.builder("size", "size", CompletionItemKind.METHOD)
                        .detail("Files.size")
                        .signature("Files.size(Path path)")
                        .returnType("long")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Returns the size of a file in bytes.")
                        .build(),

                CompletionItem.builder("probeContentType", "probeContentType", CompletionItemKind.METHOD)
                        .detail("Files.probeContentType")
                        .signature("Files.probeContentType(Path path)")
                        .returnType("String")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Probes the content type (MIME type) of a file.")
                        .build(),

                CompletionItem.builder("getLastModifiedTime", "getLastModifiedTime", CompletionItemKind.METHOD)
                        .detail("Files.getLastModifiedTime")
                        .signature("Files.getLastModifiedTime(Path path, LinkOption... options)")
                        .returnType("FileTime")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Returns the last modified time of a file.")
                        .build(),

                CompletionItem.builder("setLastModifiedTime", "setLastModifiedTime", CompletionItemKind.METHOD)
                        .detail("Files.setLastModifiedTime")
                        .signature("Files.setLastModifiedTime(Path path, FileTime time)")
                        .returnType("Path")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Sets the last modified time of a file.")
                        .build(),

                CompletionItem.builder("delete", "delete", CompletionItemKind.METHOD)
                        .detail("Files.delete")
                        .signature("Files.delete(Path path)")
                        .returnType("void")
                        .owner("java.nio.file.Files")
                        .category("Method")
                        .documentation("Deletes a file, throwing NoSuchFileException if it does not exist.")
                        .build()
        );
    }
}
