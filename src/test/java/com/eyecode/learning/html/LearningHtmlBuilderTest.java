package com.eyecode.learning.html;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

class LearningHtmlBuilderTest {

    private final LearningHtmlBuilder builder = new LearningHtmlBuilder();

    @Test
    void buildsCompleteHtmlShellWithLinkedAssets() {
        String html = builder.build("<h1>Hello</h1>");

        assertTrue(html.startsWith("<!DOCTYPE html>"));
        assertTrue(html.contains("<html>"));
        assertTrue(html.contains("<head>"));
        assertTrue(html.contains("<meta charset=\"UTF-8\">"));
        assertTrue(html.contains("rel=\"stylesheet\""));
        assertTrue(html.contains("href=\"data:text/css;base64,"));
        assertTrue(countOccurrences(html, "<script defer src=\"data:text/javascript;base64,") == 2);
        assertTrue(html.contains("<body class=\"learning-markdown\">"));
        assertTrue(html.contains("<main class=\"learning-content\">"));
        assertTrue(html.contains("<h1>Hello</h1>"));
        assertFalse(html.contains("style=\""));
    }

    @Test
    void buildsPersistentShellWithStableContentTarget() {
        String shell = builder.buildShell();

        assertTrue(shell.contains("id=\"learning-content\""));
        assertTrue(shell.contains("class=\"learning-content\""));
        assertFalse(shell.contains("<h1>"));
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(needle, index)) >= 0) {
            count++;
            index += needle.length();
        }
        return count;
    }
}
