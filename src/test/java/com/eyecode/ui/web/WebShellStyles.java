package com.eyecode.ui.web;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class WebShellStyles {
    private static final Path ENTRY = Path.of("src/main/web/src/styles/index.css");
    private static final Pattern RELATIVE_IMPORT = Pattern.compile("@import\\s+[\"'](\\.[^\"']+)[\"']");

    private WebShellStyles() {
    }

    static String all() {
        StringBuilder css = new StringBuilder();
        append(ENTRY.normalize(), css, new ArrayList<>());
        return css.toString();
    }

    private static void append(Path file, StringBuilder css, List<Path> visited) {
        Path key = file.normalize();
        if (visited.contains(key)) {
            return;
        }
        visited.add(key);
        String content;
        try {
            content = Files.readString(key);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
        css.append(content).append('\n');
        Matcher matcher = RELATIVE_IMPORT.matcher(content);
        while (matcher.find()) {
            append(key.getParent().resolve(matcher.group(1)).normalize(), css, visited);
        }
    }
}
