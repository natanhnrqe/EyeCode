package com.eyecode.filesystem;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.List;

public interface FileSystemService {

    String readFile(Path path) throws IOException;

    void writeFile(Path path, String content) throws IOException;

    boolean exists(Path path);

    void createDirectories(Path path) throws IOException;

    List<Path> listFiles(Path directory) throws IOException;

    List<Path> findFiles(Path root, String globPattern) throws IOException;

    void copyResource(InputStream source, Path target) throws IOException;
}
