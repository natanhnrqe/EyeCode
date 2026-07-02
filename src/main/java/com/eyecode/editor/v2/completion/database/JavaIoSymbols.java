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

                CompletionItem.builder("FileReader", "FileReader", CompletionItemKind.CLASS)
                        .detail("java.io.FileReader")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Reads character files, using the default system encoding. Convenience class for reading text files.")
                        .example("FileReader fr = new FileReader(\"file.txt\");")
                        .build(),

                CompletionItem.builder("FileWriter", "FileWriter", CompletionItemKind.CLASS)
                        .detail("java.io.FileWriter")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Writes character files, using the default system encoding. Convenience class for writing text files.")
                        .example("FileWriter fw = new FileWriter(\"output.txt\");")
                        .build(),

                CompletionItem.builder("PrintWriter", "PrintWriter", CompletionItemKind.CLASS)
                        .detail("java.io.PrintWriter")
                        .owner("java.io")
                        .category("Class")
                        .documentation("Prints formatted representations of objects to a text-output stream.")
                        .example("PrintWriter pw = new PrintWriter(\"output.txt\");\npw.println(\"Hello\");")
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
                        .build(),

                // -- File Methods --
                CompletionItem.builder("exists", "exists", CompletionItemKind.METHOD)
                        .detail("File.exists")
                        .signature("File.exists()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Tests whether the file or directory exists on the filesystem.")
                        .build(),

                CompletionItem.builder("isFile", "isFile", CompletionItemKind.METHOD)
                        .detail("File.isFile")
                        .signature("File.isFile()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Tests whether this pathname represents a regular file.")
                        .build(),

                CompletionItem.builder("isDirectory", "isDirectory", CompletionItemKind.METHOD)
                        .detail("File.isDirectory")
                        .signature("File.isDirectory()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Tests whether this pathname represents a directory.")
                        .build(),

                CompletionItem.builder("getName", "getName", CompletionItemKind.METHOD)
                        .detail("File.getName")
                        .signature("File.getName()")
                        .returnType("String")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Returns the name of the file or directory (last element of the path).")
                        .build(),

                CompletionItem.builder("getAbsolutePath", "getAbsolutePath", CompletionItemKind.METHOD)
                        .detail("File.getAbsolutePath")
                        .signature("File.getAbsolutePath()")
                        .returnType("String")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Returns the absolute path string of this file.")
                        .build(),

                CompletionItem.builder("getPath", "getPath", CompletionItemKind.METHOD)
                        .detail("File.getPath")
                        .signature("File.getPath()")
                        .returnType("String")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Returns the path string used to create this File object.")
                        .build(),

                CompletionItem.builder("length", "length", CompletionItemKind.METHOD)
                        .detail("File.length")
                        .signature("File.length()")
                        .returnType("long")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Returns the length of the file in bytes.")
                        .build(),

                CompletionItem.builder("canRead", "canRead", CompletionItemKind.METHOD)
                        .detail("File.canRead")
                        .signature("File.canRead()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Tests whether the application can read the file.")
                        .build(),

                CompletionItem.builder("canWrite", "canWrite", CompletionItemKind.METHOD)
                        .detail("File.canWrite")
                        .signature("File.canWrite()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Tests whether the application can write to the file.")
                        .build(),

                CompletionItem.builder("createNewFile", "createNewFile", CompletionItemKind.METHOD)
                        .detail("File.createNewFile")
                        .signature("File.createNewFile()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Atomically creates a new, empty file if it does not already exist.")
                        .build(),

                CompletionItem.builder("delete", "delete", CompletionItemKind.METHOD)
                        .detail("File.delete")
                        .signature("File.delete()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Deletes the file or directory represented by this pathname.")
                        .build(),

                CompletionItem.builder("mkdir", "mkdir", CompletionItemKind.METHOD)
                        .detail("File.mkdir")
                        .signature("File.mkdir()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Creates the directory named by this pathname.")
                        .build(),

                CompletionItem.builder("mkdirs", "mkdirs", CompletionItemKind.METHOD)
                        .detail("File.mkdirs")
                        .signature("File.mkdirs()")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Creates the directory and any necessary but nonexistent parent directories.")
                        .build(),

                CompletionItem.builder("list", "list", CompletionItemKind.METHOD)
                        .detail("File.list")
                        .signature("File.list()")
                        .returnType("String[]")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Returns an array of strings naming the files and directories in the directory.")
                        .build(),

                CompletionItem.builder("listFiles", "listFiles", CompletionItemKind.METHOD)
                        .detail("File.listFiles")
                        .signature("File.listFiles()")
                        .returnType("File[]")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Returns an array of File objects for the files/directories in the directory.")
                        .build(),

                CompletionItem.builder("renameTo", "renameTo", CompletionItemKind.METHOD)
                        .detail("File.renameTo")
                        .signature("File.renameTo(File dest)")
                        .returnType("boolean")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Renames the file represented by this pathname.")
                        .build(),

                CompletionItem.builder("lastModified", "lastModified", CompletionItemKind.METHOD)
                        .detail("File.lastModified")
                        .signature("File.lastModified()")
                        .returnType("long")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Returns the time (milliseconds since epoch) that the file was last modified.")
                        .build(),

                CompletionItem.builder("toPath", "toPath", CompletionItemKind.METHOD)
                        .detail("File.toPath")
                        .signature("File.toPath()")
                        .returnType("Path")
                        .owner("java.io.File")
                        .category("Method")
                        .documentation("Converts this File object to a java.nio.file.Path.")
                        .build(),

                // -- BufferedReader Methods --
                CompletionItem.builder("readLine", "readLine", CompletionItemKind.METHOD)
                        .detail("BufferedReader.readLine")
                        .signature("BufferedReader.readLine()")
                        .returnType("String")
                        .owner("java.io.BufferedReader")
                        .category("Method")
                        .documentation("Reads a line of text, returning null when the end of the stream is reached.")
                        .build(),

                CompletionItem.builder("read", "read", CompletionItemKind.METHOD)
                        .detail("BufferedReader.read")
                        .signature("BufferedReader.read()")
                        .returnType("int")
                        .owner("java.io.BufferedReader")
                        .category("Method")
                        .documentation("Reads a single character, returning the character as an int in the range 0-65535.")
                        .build(),

                CompletionItem.builder("lines", "lines", CompletionItemKind.METHOD)
                        .detail("BufferedReader.lines")
                        .signature("BufferedReader.lines()")
                        .returnType("Stream<String>")
                        .owner("java.io.BufferedReader")
                        .category("Method")
                        .documentation("Returns a Stream of lines read from this reader (Java 8+).")
                        .build(),

                CompletionItem.builder("close", "close", CompletionItemKind.METHOD)
                        .detail("BufferedReader.close")
                        .signature("BufferedReader.close()")
                        .returnType("void")
                        .owner("java.io.BufferedReader")
                        .category("Method")
                        .documentation("Closes the stream and releases any system resources.")
                        .build(),

                // -- BufferedWriter Methods --
                CompletionItem.builder("write", "write", CompletionItemKind.METHOD)
                        .detail("BufferedWriter.write")
                        .signature("BufferedWriter.write(String str)")
                        .returnType("void")
                        .owner("java.io.BufferedWriter")
                        .category("Method")
                        .documentation("Writes a string to the buffered writer.")
                        .build(),

                CompletionItem.builder("newLine", "newLine", CompletionItemKind.METHOD)
                        .detail("BufferedWriter.newLine")
                        .signature("BufferedWriter.newLine()")
                        .returnType("void")
                        .owner("java.io.BufferedWriter")
                        .category("Method")
                        .documentation("Writes a line separator to the buffered writer.")
                        .build(),

                CompletionItem.builder("flush", "flush", CompletionItemKind.METHOD)
                        .detail("BufferedWriter.flush")
                        .signature("BufferedWriter.flush()")
                        .returnType("void")
                        .owner("java.io.BufferedWriter")
                        .category("Method")
                        .documentation("Flushes the buffered writer, writing any buffered data to the underlying stream.")
                        .build(),

                // -- PrintWriter Methods --
                CompletionItem.builder("print", "print", CompletionItemKind.METHOD)
                        .detail("PrintWriter.print")
                        .signature("PrintWriter.print(String s)")
                        .returnType("void")
                        .owner("java.io.PrintWriter")
                        .category("Method")
                        .documentation("Prints a string without a trailing newline.")
                        .build(),

                CompletionItem.builder("println", "println", CompletionItemKind.METHOD)
                        .detail("PrintWriter.println")
                        .signature("PrintWriter.println(String x)")
                        .returnType("void")
                        .owner("java.io.PrintWriter")
                        .category("Method")
                        .documentation("Prints a string and terminates the line.")
                        .build(),

                CompletionItem.builder("printf", "printf", CompletionItemKind.METHOD)
                        .detail("PrintWriter.printf")
                        .signature("PrintWriter.printf(String format, Object... args)")
                        .returnType("PrintWriter")
                        .owner("java.io.PrintWriter")
                        .category("Method")
                        .documentation("Writes a formatted string using printf-style format specifiers.")
                        .build(),

                CompletionItem.builder("format", "format", CompletionItemKind.METHOD)
                        .detail("PrintWriter.format")
                        .signature("PrintWriter.format(String format, Object... args)")
                        .returnType("PrintWriter")
                        .owner("java.io.PrintWriter")
                        .category("Method")
                        .documentation("Writes a formatted string using printf-style format specifiers.")
                        .build(),

                CompletionItem.builder("flush", "flush", CompletionItemKind.METHOD)
                        .detail("PrintWriter.flush")
                        .signature("PrintWriter.flush()")
                        .returnType("void")
                        .owner("java.io.PrintWriter")
                        .category("Method")
                        .documentation("Flushes the stream.")
                        .build(),

                CompletionItem.builder("close", "close", CompletionItemKind.METHOD)
                        .detail("PrintWriter.close")
                        .signature("PrintWriter.close()")
                        .returnType("void")
                        .owner("java.io.PrintWriter")
                        .category("Method")
                        .documentation("Closes the stream and releases any system resources.")
                        .build(),

                CompletionItem.builder("checkError", "checkError", CompletionItemKind.METHOD)
                        .detail("PrintWriter.checkError")
                        .signature("PrintWriter.checkError()")
                        .returnType("boolean")
                        .owner("java.io.PrintWriter")
                        .category("Method")
                        .documentation("Flushes the stream and checks if an error has occurred.")
                        .build()
        );
    }
}
