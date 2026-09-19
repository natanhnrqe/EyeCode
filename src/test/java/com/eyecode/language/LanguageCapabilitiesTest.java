package com.eyecode.language;

import com.eyecode.language.completion.CompletionCandidate;
import com.eyecode.language.completion.CompletionProvider;
import com.eyecode.language.completion.CompletionRequest;
import com.eyecode.language.completion.CompletionResult;
import com.eyecode.language.completion.CompletionService;
import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import com.eyecode.language.diagnostics.DiagnosticsProvider;
import com.eyecode.language.diagnostics.DiagnosticsRequest;
import com.eyecode.language.diagnostics.DiagnosticsResult;
import com.eyecode.language.diagnostics.DiagnosticsService;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageCapabilitiesTest {
    private final DocumentLanguageResolver resolver = new ExtensionDocumentLanguageResolver(
            Map.of(LanguageId.JAVA, Set.of("java")));

    @Test
    void resolvesFileNamesAndDeclaredLessonLanguagesWithoutAPath() {
        assertEquals(LanguageId.JAVA, resolver.resolve(new LanguageDocument("file:///Main.java",
                Path.of("Main.java"), "Main.java", null)).orElseThrow());
        assertEquals(LanguageId.JAVA, resolver.resolve(new LanguageDocument("lesson://fundamentals/one/main",
                null, "main", LanguageId.JAVA)).orElseThrow());
        assertTrue(resolver.resolve(new LanguageDocument("untitled://one", null, "Untitled", null)).isEmpty());
    }

    @Test
    void declaredLanguageWinsOverExtensionEvenWhenNoProviderIsRegistered() {
        LanguageId foo = new LanguageId("foo");
        LanguageDocument document = new LanguageDocument("file:///Main.java", Path.of("Main.java"), "Main.java", foo);

        assertEquals(foo, resolver.resolve(document).orElseThrow());
    }

    @Test
    void routesDiagnosticsOnlyToTheResolvedLanguage() {
        DiagnosticsService service = new DiagnosticsService(resolver, List.of(new TestDiagnosticsProvider()));
        DiagnosticsResult javaResult = service.analyze(new DiagnosticsRequest(javaDocument(), 4, "class Main {}"));
        DiagnosticsResult unknownResult = service.analyze(new DiagnosticsRequest(
                new LanguageDocument("file:///note.txt", Path.of("note.txt"), "note.txt", null), 1, "note"));

        assertEquals(1, javaResult.diagnostics().size());
        assertTrue(unknownResult.diagnostics().isEmpty());
        assertFalse(unknownResult.hasInfrastructureError());
    }

    @Test
    void routesCompletionOnlyToTheResolvedLanguage() {
        CompletionService service = new CompletionService(resolver, List.of(new TestCompletionProvider()));
        CompletionResult javaResult = service.complete(new CompletionRequest(javaDocument(), 3, "cla", 3, true, -1, -1));
        CompletionResult unknownResult = service.complete(new CompletionRequest(
                new LanguageDocument("file:///note.txt", Path.of("note.txt"), "note.txt", null), 1, "x", 1, true, -1, -1));

        assertEquals("class", javaResult.candidates().getFirst().label());
        assertTrue(unknownResult.candidates().isEmpty());
    }

    @Test
    void diagnosticsCanBeRegisteredWithoutCompletionForTheSameLanguage() {
        LanguageId foo = new LanguageId("foo");
        DocumentLanguageResolver fooResolver = new ExtensionDocumentLanguageResolver(Map.of(
                LanguageId.JAVA, Set.of("java"), foo, Set.of("foo")));
        LanguageDocument document = new LanguageDocument("untitled://foo", null, "Untitled", foo);
        DiagnosticsService diagnostics = new DiagnosticsService(fooResolver, List.of(new FooDiagnosticsProvider(foo)));
        CompletionService completion = new CompletionService(fooResolver, List.of());

        assertEquals(1, diagnostics.analyze(new DiagnosticsRequest(document, 1, "source")).diagnostics().size());
        assertTrue(completion.complete(new CompletionRequest(document, 1, "source", 0, true, -1, -1))
                .candidates().isEmpty());
    }

    private static LanguageDocument javaDocument() {
        return new LanguageDocument("file:///Main.java", Path.of("Main.java"), "Main.java", null);
    }

    private static final class TestDiagnosticsProvider implements DiagnosticsProvider {
        @Override public LanguageId languageId() { return LanguageId.JAVA; }
        @Override public DiagnosticsResult analyze(DiagnosticsRequest request) {
            return new DiagnosticsResult(List.of(new Diagnostic(DiagnosticSeverity.INFO, "test", "ok", 1, 1, 1, 2)), "");
        }
    }

    private static final class TestCompletionProvider implements CompletionProvider {
        @Override public LanguageId languageId() { return LanguageId.JAVA; }
        @Override public CompletionResult complete(CompletionRequest request) {
            return new CompletionResult(List.of(new CompletionCandidate("class", "KEYWORD", "", "", "class", "class",
                    false, 0, 3, 1, "", "", "", "", "", List.of())));
        }
    }

    private record FooDiagnosticsProvider(LanguageId languageId) implements DiagnosticsProvider {
        @Override public DiagnosticsResult analyze(DiagnosticsRequest request) {
            return new DiagnosticsResult(List.of(new Diagnostic(DiagnosticSeverity.INFO, "foo", "ok", 1, 1, 1, 2)), "");
        }
    }
}
