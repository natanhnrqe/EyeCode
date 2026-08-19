package com.eyecode.editor.v2.completion.semantic;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.EditorSelection;
import com.eyecode.editor.v2.completion.CompletionEngine;
import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionSnapshot;
import com.eyecode.editor.v2.completion.JavaKeywordCompletionProvider;
import com.eyecode.editor.v2.completion.JavaSnippetProvider;
import com.eyecode.editor.v2.diagnostics.DiagnosticSnapshot;
import com.eyecode.editor.v2.language.LanguageContext;
import com.eyecode.editor.v2.syntax.JavaSyntaxAnalyzer;
import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SemanticCompletionProviderTest {

    private static final Path SOURCE = Path.of("SemanticCompletionTest.java");

    private final SemanticCompletionProvider provider = new SemanticCompletionProvider(new SemanticSymbolRegistry());
    private final JavaSyntaxAnalyzer syntaxAnalyzer = new JavaSyntaxAnalyzer();

    @Test
    void localVariableCompletionIncludesLocal() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    void test() {
                        int value = 1;
                        val|
                    }
                }
                """));

        assertTrue(labels(snapshot).contains("value"));
    }

    @Test
    void parameterCompletionIncludesParameter() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    void test(String name) {
                        na|
                    }
                }
                """));

        assertTrue(labels(snapshot).contains("name"));
    }

    @Test
    void fieldCompletionIncludesField() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    int field;
                    void test() {
                        fie|
                    }
                }
                """));

        assertTrue(labels(snapshot).contains("field"));
    }

    @Test
    void methodCompletionIncludesMethod() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    void calculate() { }
                    void test() {
                        cal|
                    }
                }
                """));

        assertTrue(labels(snapshot).contains("calculate"));
    }

    @Test
    void typeCompletionIncludesCurrentType() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    Exa|
                }
                """));

        assertTrue(labels(snapshot).contains("Example"));
    }

    @Test
    void keywordCompletionStillFlowsThroughSharedEngine() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("""
                class Example {
                    pri|
                }
                """));

        assertTrue(labels(snapshot).contains("private"));
    }

    @Test
    void explicitCompletionWithEmptyPrefixReturnsVisibleSymbolsKeywordsAndSnippets() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSnippetProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("""
                class Example {
                    int field;
                    void helper() { }
                    void test(int parameter) {
                        |
                    }
                }
                """), true);

        List<String> labels = labels(snapshot);
        assertTrue(labels.contains("parameter"));
        assertTrue(labels.contains("field"));
        assertTrue(labels.contains("helper"));
        assertTrue(labels.contains("if"));
        assertTrue(labels.contains("sout"));
    }

    @Test
    void explicitCompletionAtClassBodyWithEmptyPrefixReturnsContextCandidates() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSnippetProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("""
                class Example {
                    |
                }
                """), true);

        List<String> labels = labels(snapshot);
        assertTrue(labels.contains("class"));
        assertTrue(labels.contains("interface"));
        assertTrue(labels.contains("record"));
        assertTrue(labels.contains("Example"));
    }

    @Test
    void explicitCompletionAfterWhitespaceReturnsVisibleSymbols() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    int field;
                    void test(int parameter) {

                        |
                    }
                }
                """), true);

        List<String> labels = labels(snapshot);
        assertTrue(labels.contains("parameter"));
        assertTrue(labels.contains("field"));
    }

    @Test
    void prefixFilteringKeepsOnlyMatchingItems() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    void printValue() { }
                    void test() {
                        int value = 1;
                        pr|
                    }
                }
                """));

        List<String> labels = labels(snapshot);
        assertTrue(labels.contains("printValue"));
        assertFalse(labels.contains("value"));
    }

    @Test
    void shadowingDoesNotProduceDuplicateVisibleNames() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    int value;
                    void test(int value) {
                        val|
                    }
                }
                """));

        assertEquals(List.of("value"), labels(snapshot));
    }

    @Test
    void unresolvedContextIsSafe() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Example {
                    void test() {
                        ??|
                    }
                }
                """));

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void resultOrderIsDeterministic() {
        LanguageContext context = context("""
                class Example {
                    int field;
                    void alpha() { }
                    void beta() { }
                    void test(int parameter) {
                        int local = 1;
                        |
                    }
                }
                """);

        List<String> first = labels(provider.complete(context));
        List<String> second = labels(provider.complete(context));
        assertIterableEquals(first, second);
    }

    @Test
    void staticQualifiedCompletionReturnsStaticMembersOnly() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Helper {
                    static int count;
                    static void ping() { }
                    int hidden;
                }
                class Example {
                    void test() {
                        Helper.|
                    }
                }
                """));

        List<String> labels = labels(snapshot);
        assertTrue(labels.contains("count"));
        assertTrue(labels.contains("ping"));
        assertFalse(labels.contains("hidden"));
    }

    @Test
    void objectQualifiedCompletionDoesNotFabricateMembers() {
        CompletionSnapshot snapshot = provider.complete(context("""
                class Helper {
                    void ping() { }
                }
                class Example {
                    void test() {
                        Helper helper = new Helper();
                        helper.|
                    }
                }
                """));

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void explicitCompletionInsideCommentRemainsSuppressed() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSnippetProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("""
                class Example {
                    void test() {
                        // |
                    }
                }
                """), true);

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void explicitCompletionInsideStringRemainsSuppressed() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSnippetProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("""
                class Example {
                    String value = "|";
                }
                """), true);

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void explicitCompletionInsideCharLiteralRemainsSuppressed() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSnippetProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("""
                class Example {
                    char value = '|';
                }
                """), true);

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void automaticCompletionWithEmptyPrefixRemainsUnchanged() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSnippetProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("""
                class Example {
                    void test() {
                        |
                    }
                }
                """), false);

        assertTrue(snapshot.isEmpty());
    }

    @Test
    void explicitCompletionOnEmptyDocumentIsAvailable() {
        CompletionEngine engine = new CompletionEngine(List.of(
                new JavaKeywordCompletionProvider(),
                new JavaSnippetProvider(),
                provider
        ));

        CompletionSnapshot snapshot = engine.complete(context("|"), true);

        List<String> labels = labels(snapshot);
        assertFalse(labels.isEmpty());
        assertTrue(labels.contains("class"));
        assertTrue(labels.contains("sout"));
    }

    private LanguageContext context(String sourceWithCaret) {
        int offset = sourceWithCaret.indexOf('|');
        String source = sourceWithCaret.substring(0, offset) + sourceWithCaret.substring(offset + 1);
        EditorDocument document = new EditorDocument(SOURCE, source);
        EditorPosition caret = document.positionOf(offset);
        EditorSelection selection = new EditorSelection(caret, caret);
        SyntaxSnapshot syntax = syntaxAnalyzer.analyze(document);
        return new LanguageContext(document, caret, selection, syntax, DiagnosticSnapshot.empty());
    }

    private List<String> labels(CompletionSnapshot snapshot) {
        return snapshot.getItems().stream()
                .map(CompletionItem::getLabel)
                .toList();
    }
}
