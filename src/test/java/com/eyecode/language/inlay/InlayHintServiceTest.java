package com.eyecode.language.inlay;

import com.eyecode.language.DocumentLanguageResolver;
import com.eyecode.language.ExtensionDocumentLanguageResolver;
import com.eyecode.language.LanguageDocument;
import com.eyecode.language.LanguageId;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InlayHintServiceTest {

    private final DocumentLanguageResolver resolver = new ExtensionDocumentLanguageResolver(
            Map.of(LanguageId.JAVA, Set.of("java")));

    @Test
    void javaDocument_withProvider_returnsResult() {
        InlayHintProvider provider = new StubProvider(LanguageId.JAVA, new InlayHintResult(
                List.of(new InlayHint(10, "left:"))));
        InlayHintService service = new InlayHintService(resolver, List.of(provider));

        Optional<InlayHintResult> result = service.inlayHints(request());

        assertTrue(result.isPresent());
        assertEquals(1, result.get().hints().size());
        assertEquals(10, result.get().hints().getFirst().offset());
        assertEquals("left:", result.get().hints().getFirst().label());
    }

    @Test
    void documentWithoutMatchingLanguage_returnsEmpty() {
        InlayHintProvider provider = new StubProvider(LanguageId.JAVA, new InlayHintResult(List.of()));
        InlayHintService service = new InlayHintService(resolver, List.of(provider));
        LanguageDocument text = new LanguageDocument("file:///Notes.txt", Path.of("Notes.txt"),
                "Notes.txt", null);

        Optional<InlayHintResult> result = service.inlayHints(
                new InlayHintRequest(text, 1L, "hello", 0, 5, InlayHintMode.NAME));

        assertTrue(result.isEmpty());
    }

    @Test
    void languageWithoutProvider_returnsEmpty() {
        InlayHintService service = new InlayHintService(resolver, List.of());

        assertTrue(service.inlayHints(request()).isEmpty());
    }

    @Test
    void duplicateProviderForSameLanguage_rejected() {
        InlayHintProvider first = new StubProvider(LanguageId.JAVA, new InlayHintResult(List.of()));
        InlayHintProvider second = new StubProvider(LanguageId.JAVA, new InlayHintResult(List.of()));

        assertThrows(IllegalArgumentException.class,
                () -> new InlayHintService(resolver, List.of(first, second)));
    }

    @Test
    void nullRequest_rejected() {
        InlayHintService service = new InlayHintService(resolver, List.of());

        assertThrows(IllegalArgumentException.class, () -> service.inlayHints(null));
    }

    @Test
    void nullHintList_becomesEmptyResult() {
        InlayHintResult result = new InlayHintResult(null);

        assertTrue(result.hints().isEmpty());
    }

    @Test
    void hintResult_isDefensivelyCopied() {
        List<InlayHint> source = new java.util.ArrayList<>(List.of(new InlayHint(1, "a:")));
        InlayHintResult result = new InlayHintResult(source);
        source.add(new InlayHint(2, "b:"));

        assertEquals(1, result.hints().size());
        assertThrows(UnsupportedOperationException.class, () -> result.hints().add(new InlayHint(3, "c:")));
    }

    private static InlayHintRequest request() {
        LanguageDocument document = new LanguageDocument("file:///Demo.java", Path.of("Demo.java"),
                "Demo.java", LanguageId.JAVA);
        return new InlayHintRequest(document, 1L, "class Demo {}", 0, 13, InlayHintMode.NAME);
    }

    private record StubProvider(LanguageId id, InlayHintResult result) implements InlayHintProvider {
        @Override
        public LanguageId languageId() {
            return id;
        }

        @Override
        public InlayHintResult inlayHints(InlayHintRequest request) {
            return result;
        }
    }
}
