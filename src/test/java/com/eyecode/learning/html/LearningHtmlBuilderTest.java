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
        assertTrue(html.contains("<body>"));
        assertTrue(html.contains("<h1>Hello</h1>"));
        assertFalse(html.contains("style=\""));
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
