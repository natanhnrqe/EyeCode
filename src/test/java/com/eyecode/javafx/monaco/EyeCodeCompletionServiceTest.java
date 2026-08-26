package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.ContextAwareCompletionProvider;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.JavaStandardLibraryProvider;
import com.eyecode.editor.v2.completion.JavaSnippetProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticCompletionProvider;
import com.eyecode.editor.v2.completion.semantic.SemanticSymbolRegistry;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EyeCodeCompletionServiceTest {

    private final EyeCodeCompletionService service = new EyeCodeCompletionService(
            new CompletionEngine(List.of(
                    new SemanticCompletionProvider(new SemanticSymbolRegistry()),
                    new ContextAwareCompletionProvider(),
                    new JavaKeywordCompletionProvider(),
                    new JavaStandardLibraryProvider(),
                    new JavaSnippetProvider())));

    @Test
    void prefixCompletionUsesEyeCodeItemsAndReplacementRange() {
        EditorDocument document = new EditorDocument(null, "Str");
        MonacoCompletionRequest request = request(document, 1, 4, false,
                MonacoCompletionRequest.TriggerKind.INVOKED);

        List<MonacoCompletionItem> items = service.complete(request, context(document, 3));

        MonacoCompletionItem string = items.stream()
                .filter(item -> item.label().equals("String"))
                .findFirst().orElseThrow();
        assertEquals(0, string.replaceStart());
        assertEquals(3, string.replaceEnd());
    }

    @Test
    void explicitEmptyPrefixReturnsKeywords() {
        EditorDocument document = new EditorDocument(null, "");
        MonacoCompletionRequest request = request(document, 1, 1, true,
                MonacoCompletionRequest.TriggerKind.INVOKED);

        List<MonacoCompletionItem> items = service.complete(request, context(document, 0));

        assertTrue(items.stream().anyMatch(item -> item.label().equals("public")));
    }

    @Test
    void dotCompletionReturnsMembersAndReplacesOnlyTerminalPrefix() {
        EditorDocument document = new EditorDocument(null, "System.ou");
        MonacoCompletionRequest request = request(document, 1, 10, false,
                MonacoCompletionRequest.TriggerKind.TRIGGER_CHARACTER);

        List<MonacoCompletionItem> items = service.complete(request, context(document, 9));

        MonacoCompletionItem output = items.stream()
                .filter(item -> item.label().equals("out"))
                .findFirst().orElseThrow();
        assertEquals(7, output.replaceStart());
        assertEquals(9, output.replaceEnd());
    }

    @Test
    void unicodeBeforeCaretUsesUtf16DocumentOffsets() {
        EditorDocument document = new EditorDocument(null, "😀\r\nStr");
        MonacoCompletionRequest request = request(document, 2, 4, false,
                MonacoCompletionRequest.TriggerKind.INVOKED);

        List<MonacoCompletionItem> items = service.complete(request, context(document, 7));

        MonacoCompletionItem string = items.stream()
                .filter(item -> item.label().equals("String"))
                .findFirst().orElseThrow();
        assertEquals(4, string.replaceStart());
        assertEquals(7, string.replaceEnd());
    }

    @Test
    void automaticEmptyPrefixRemainsEmpty() {
        EditorDocument document = new EditorDocument(null, "");
        MonacoCompletionRequest request = request(document, 1, 1, false,
                MonacoCompletionRequest.TriggerKind.INVOKED);

        assertFalse(service.complete(request, context(document, 0)).stream()
                .anyMatch(item -> item.label().equals("public")));
    }

    private static MonacoCompletionRequest request(EditorDocument document, int line, int column,
                                                   boolean explicit,
                                                   MonacoCompletionRequest.TriggerKind trigger) {
        return new MonacoCompletionRequest("file:///Test.java", document.currentVersion(),
                line, column, trigger, trigger == MonacoCompletionRequest.TriggerKind.TRIGGER_CHARACTER
                ? "." : null, 1L, explicit);
    }

    private static LanguageContext context(EditorDocument document, int offset) {
        EditorPosition position = document.positionOf(offset);
        return new LanguageContext(document, position, new EditorSelection(position, position),
                new JavaSyntaxAnalyzer().analyze(document), DiagnosticSnapshot.empty());
    }
}
