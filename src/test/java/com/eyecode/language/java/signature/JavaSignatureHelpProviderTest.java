package com.eyecode.language.java.signature;

import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageFeatureRequest;
import com.eyecode.language.LanguageId;
import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.language.signature.SignatureHelpResult;
import com.eyecode.project.ProjectLifecycleService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaSignatureHelpProviderTest {

    @Test
    void jdtNull_fallsBackToLocalResolver() {
        JavaSignatureHelpProvider provider = new JavaSignatureHelpProvider(null);
        String marked = source("int sum = ad|d(1, 2);");
        int caret = text(marked).indexOf("add(") + "add(".length();

        Optional<SignatureHelpResult> result = provider.signatureHelp(request(text(marked), caret));

        assertTrue(result.isPresent());
        assertEquals("add(int, int)", result.get().signatures().getFirst().label());
        assertEquals(2, result.get().signatures().getFirst().parameters().size());
    }

    @Test
    void jdtServiceWithoutProject_fallsBackToLocalResolver() {
        JdtLsProjectService jdt = new JdtLsProjectService(new ProjectLifecycleService());
        try {
            JavaSignatureHelpProvider provider = new JavaSignatureHelpProvider(jdt);
            String marked = source("int sum = ad|d(1, 2);");
            int caret = text(marked).indexOf("add(") + "add(".length();

            Optional<SignatureHelpResult> result = provider.signatureHelp(request(text(marked), caret));

            assertTrue(result.isPresent());
            assertEquals("add(int, int)", result.get().signatures().getFirst().label());
        } finally {
            jdt.close();
        }
    }

    @Test
    void caretOutsideCall_returnsEmpty() {
        JavaSignatureHelpProvider provider = new JavaSignatureHelpProvider(null);
        String source = source("int sum = 1;");

        assertTrue(provider.signatureHelp(request(source, source.indexOf("sum"))).isEmpty());
    }

    @Test
    void nullSource_returnsEmpty() {
        JavaSignatureHelpProvider provider = new JavaSignatureHelpProvider(null);
        LanguageDocument document = new LanguageDocument("file:///Demo.java", Path.of("Demo.java"),
                "Demo.java", LanguageId.JAVA);

        assertTrue(provider.signatureHelp(new LanguageFeatureRequest(document, 1L, null, 0)).isEmpty());
    }

    private static LanguageFeatureRequest request(String source, int offset) {
        LanguageDocument document = new LanguageDocument("file:///Demo.java", Path.of("Demo.java"),
                "Demo.java", LanguageId.JAVA);
        return new LanguageFeatureRequest(document, 1L, source, offset);
    }

    private static String source(String statement) {
        return "class Calculator {\n"
                + "    int add(int left, int right) {\n"
                + "        return left + right;\n"
                + "    }\n"
                + "\n"
                + "    void run() {\n"
                + "        " + statement + "\n"
                + "    }\n"
                + "}";
    }

    private static String text(String marked) {
        return marked.replace("|", "");
    }
}
