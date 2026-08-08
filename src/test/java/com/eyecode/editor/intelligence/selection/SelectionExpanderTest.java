package com.eyecode.editor.intelligence.selection;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Level ladder of {@link JavaSelectionExpander} against the 20 cases of the
 * Sprint 5.1e spec. All levels are pure lexical heuristics over the snapshot:
 * word → expression → arguments → delimiters → statement → block →
 * declaration.
 */
class SelectionExpanderTest {

    private static final String METHOD = """
            int foo(int x, int y) {
                int z = bar(x + 1) * 2;
                return z;
            }
            """;

    private static final JavaSelectionExpander EXPANDER = new JavaSelectionExpander();

    private static DocumentSnapshot snapshot(String text) {
        return new DocumentSnapshot(0, text, LineMap.of(text), null);
    }

    private static Optional<TextRange> at(String text, int caret, int level) {
        return EXPANDER.expand(snapshot(text), caret, Optional.empty(), level);
    }

    private static Optional<TextRange> from(String text, int caret, TextRange base, int level) {
        return EXPANDER.expand(snapshot(text), caret, Optional.of(base), level);
    }

    private static void assertRange(String text, int start, int end, Optional<TextRange> actual) {
        assertTrue(actual.isPresent(), "expected (" + start + "," + end + ") but got empty");
        assertEquals(new TextRange(start, end), actual.get());
        assertEquals(text.substring(start, end), text.substring(actual.get().startOffset(), actual.get().endOffset()));
    }

    @Test
    void wordAtCaretInsideWord() {
        assertRange(METHOD, 40, 41, at(METHOD, 40, 1));
    }

    @Test
    void wordAtCaretInWordMiddle() {
        assertRange(METHOD, 4, 7, at(METHOD, 5, 1));
    }

    @Test
    void wordAtBetweenSpacesIsEmpty() {
        assertTrue(at("int   x;", 4, 1).isEmpty());
    }

    @Test
    void wordAtInsideCommentIsEmpty() {
        assertTrue(at("// x + y", 4, 1).isEmpty());
    }

    @Test
    void wordAtOutOfBoundsIsEmpty() {
        assertTrue(at(METHOD, 1000, 1).isEmpty());
    }

    @Test
    void expressionExtendsAcrossOperators() {
        assertRange(METHOD, 40, 45, at(METHOD, 40, 2));
    }

    @Test
    void expressionIncludesCallSuffixAndOperatorChain() {
        assertRange(METHOD, 36, 50, at(METHOD, 36, 2));
    }

    @Test
    void expressionStopsAtStatementBoundary() {
        assertRange(METHOD, 56, 64, at(METHOD, 63, 2));
    }

    @Test
    void expressionCoversDotChain() {
        assertRange("foo.bar();", 0, 9, at("foo.bar();", 4, 2));
    }

    @Test
    void argsContentGrowsOverMultiArgumentList() {
        assertRange("foo(a + b, c);", 4, 12, from("foo(a + b, c);", 4, new TextRange(4, 9), 3));
    }

    @Test
    void argsContentDegeneratesToEmptyForSingleArgument() {
        assertTrue(from(METHOD, 40, new TextRange(40, 45), 3).isEmpty());
    }

    @Test
    void argsContentCoversInitializerBraces() {
        assertRange("int[] a = {1, 2, 3};", 11, 18, from("int[] a = {1, 2, 3};", 11, new TextRange(11, 12), 3));
    }

    @Test
    void delimiterWrapIncludesSingleArgumentParens() {
        assertRange(METHOD, 39, 46, from(METHOD, 40, new TextRange(40, 45), 4));
    }

    @Test
    void delimiterWrapIncludesInitializerBraces() {
        assertRange("int[] a = {1, 2, 3};", 10, 19, from("int[] a = {1, 2, 3};", 11, new TextRange(11, 18), 4));
    }

    @Test
    void statementSpansToSemicolon() {
        assertRange(METHOD, 28, 51, from(METHOD, 40, new TextRange(39, 46), 5));
    }

    @Test
    void statementInsideForHeaderStopsAtSemicolon() {
        assertRange("for (int i = 0; i < n; i++) { foo(); }", 16, 28,
                from("for (int i = 0; i < n; i++) { foo(); }", 17, new TextRange(16, 21), 5));
    }

    @Test
    void statementWithoutSemicolonRunsToEnd() {
        assertRange("int z = 5", 0, 9, from("int z = 5", 4, new TextRange(4, 5), 5));
    }

    @Test
    void blockIncludesBraces() {
        assertRange(METHOD, 22, 67, from(METHOD, 40, new TextRange(39, 46), 6));
    }

    @Test
    void blockSkipsInitializerBraces() {
        assertTrue(from("int[] a = {1, 2, 3};", 11, new TextRange(11, 12), 6).isEmpty());
    }

    @Test
    void blockAtTopLevelIsEmpty() {
        assertTrue(from("int x = 1;", 4, new TextRange(4, 5), 6).isEmpty());
    }

    @Test
    void declarationSpansMethodHeaderToClosingBrace() {
        assertRange(METHOD, 0, 67, from(METHOD, 40, new TextRange(39, 46), 7));
    }

    @Test
    void declarationWithModifiersFindsHeader() {
        assertRange("public final void go() {\n    x();\n}", 0, 35,
                from("public final void go() {\n    x();\n}", 29, new TextRange(29, 30), 7));
    }

    @Test
    void declarationWithoutBlockSpansWholeDocument() {
        assertRange("int x = 1;", 0, 10, from("int x = 1;", 4, new TextRange(4, 5), 7));
    }

    @Test
    void outOfRangeLevelIsEmpty() {
        assertTrue(at(METHOD, 36, 0).isEmpty());
        assertTrue(at(METHOD, 36, 8).isEmpty());
    }

    @Test
    void nullSnapshotIsEmpty() {
        assertTrue(EXPANDER.expand(null, 0, Optional.empty(), 1).isEmpty());
    }

    @Test
    void expandDoesNotMutateSnapshot() {
        DocumentSnapshot snapshot = snapshot(METHOD);
        from(METHOD, 36, new TextRange(35, 42), 7);
        assertEquals(METHOD, snapshot.getText());
    }
}
