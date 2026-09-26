package com.eyecode.language.java.hover;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.hover.HoverResult;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.project.ProjectLifecycleService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaHoverProviderTest {

    @Test
    void jdtNull_localJavadocRendersHtmlContent() {
        JavaHoverProvider provider = new JavaHoverProvider(null);
        String marked = source("int sum = ad|d(1, 2);");

        Optional<HoverResult> result = provider.hover(request(text(marked), caret(marked)));

        assertTrue(result.isPresent());
        assertEquals(1, result.get().contents().size());
        assertEquals("html", result.get().contents().getFirst().kind());
        assertTrue(result.get().contents().getFirst().value().contains("Adds two values together."));
        assertEquals("add", text(marked).substring(result.get().rangeStart(), result.get().rangeEnd()));
    }

    @Test
    void jdtNull_unknownSymbolFallsBackToDatabaseDocumentation() {
        JavaHoverProvider provider = new JavaHoverProvider(null);
        String marked = source("text.sub|string(0, 3);");

        Optional<HoverResult> result = provider.hover(request(text(marked), caret(marked)));

        assertTrue(result.isPresent());
        assertTrue(result.get().contents().getFirst().value().contains("substring of this string"));
        assertEquals("substring", text(marked).substring(result.get().rangeStart(), result.get().rangeEnd()));
    }

    @Test
    void jdtNull_keywordFallsBackToKnowledgeBaseDocumentation() {
        JavaHoverProvider provider = new JavaHoverProvider(null);
        String marked = source("swi|tch (result) {");

        Optional<HoverResult> result = provider.hover(request(text(marked), caret(marked)));

        assertTrue(result.isPresent());
        assertTrue(result.get().contents().getFirst().value().contains("Multi-branch selection"));
    }

    @Test
    void jdtNull_localVariableWithoutDocs_returnsEmpty() {
        JavaHoverProvider provider = new JavaHoverProvider(null);
        String marked = source("int outcome = res|ult;");

        assertTrue(provider.hover(request(text(marked), caret(marked))).isEmpty());
    }

    @Test
    void jdtNull_caretInWhitespace_returnsEmpty() {
        JavaHoverProvider provider = new JavaHoverProvider(null);
        String marked = source("int sum = add(1, |2);");

        assertTrue(provider.hover(request(text(marked), caret(marked))).isEmpty());
    }

    @Test
    void jdtServiceWithoutProject_fallsBackToLocalJavadoc() {
        JdtLsProjectService jdt = new JdtLsProjectService(new ProjectLifecycleService());
        try {
            JavaHoverProvider provider = new JavaHoverProvider(jdt);
            String marked = source("int sum = ad|d(1, 2);");

            Optional<HoverResult> result = provider.hover(request(text(marked), caret(marked)));

            assertTrue(result.isPresent());
            assertTrue(result.get().contents().getFirst().value().contains("Adds two values together."));
        } finally {
            jdt.close();
        }
    }

    private static LanguageFeatureRequest request(String source, int offset) {
        LanguageDocument document = new LanguageDocument("file:///Demo.java", Path.of("Demo.java"),
                "Demo.java", LanguageId.JAVA);
        return new LanguageFeatureRequest(document, 1L, source, offset);
    }

    private static String source(String statement) {
        return "class Calculator {\n"
                + "    /** Adds two values together. */\n"
                + "    int add(int left, int right) {\n"
                + "        return left + right;\n"
                + "    }\n"
                + "\n"
                + "    void run() {\n"
                + "        String text = \"EyeCode\";\n"
                + "        int result = 1;\n"
                + "        " + statement + "\n"
                + "    }\n"
                + "}";
    }

    private static String text(String marked) {
        return marked.replace("|", "");
    }

    private static int caret(String marked) {
        return marked.indexOf('|');
    }
}
