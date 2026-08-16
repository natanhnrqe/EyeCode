package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 5.4c.2 — DefinitionReferenceResolver tests.
 * <p>
 * Validates that the caret-driven reference extractor produces the
 * right {@link SymbolReference} (or none) for the full set of
 * spec §8 scenarios: simple identifiers, second-occurrence references,
 * whitespace / comment / string / character exclusions, out-of-document
 * offsets, end-of-identifier offsets, Unicode identifiers, qualified
 * names with 2 and 3 components, and {@code this.field} (which is a
 * {@code KEYWORD . IDENTIFIER} lexically).
 * <p>
 * Source-string-driven with offset positions derived from the actual
 * lexer output (not hardcoded numbers — see the diagnostic that
 * preceded these tests).
 */
class DefinitionReferenceResolverTest {

    private record Pipeline(SymbolTable table, String source) {}

    private Pipeline build(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Pipeline(sem.symbolTable(), source);
    }

    private Optional<SymbolReference> resolveAt(Pipeline p, int offset) {
        return new DefinitionReferenceResolver().resolve(p.source, offset, p.table);
    }

    // ----------------------------------------------------------------------
    // 1. simple identifier
    // ----------------------------------------------------------------------
    @Test
    void simpleIdentifier() {
        String source = "int value = 10;";
        Pipeline p = build(source);
        // `value` is at offset [4, 9).
        Optional<SymbolReference> ref = resolveAt(p, 4);
        assertTrue(ref.isPresent());
        SymbolReference r = ref.get();
        assertEquals("value", r.name());
        assertEquals(TextRange.of(4, 9), r.range());
        assertEquals(SymbolReferenceKind.SIMPLE, r.kind());
    }

    // ----------------------------------------------------------------------
    // 2. second occurrence
    // ----------------------------------------------------------------------
    @Test
    void secondOccurrence() {
        String source = "int value = 10;\nvalue++;";
        Pipeline p = build(source);
        // First `value` at [4, 9); second `value` at [16, 21).
        int offset2 = 16;
        Optional<SymbolReference> ref = resolveAt(p, offset2);
        assertTrue(ref.isPresent());
        SymbolReference r = ref.get();
        assertEquals("value", r.name());
        assertEquals(TextRange.of(16, 21), r.range());
    }

    // ----------------------------------------------------------------------
    // 3. whitespace
    // ----------------------------------------------------------------------
    @Test
    void whitespace_returnsEmpty() {
        String source = "int   value = 10;";
        Pipeline p = build(source);
        // The third char (index 3) is whitespace.
        Optional<SymbolReference> ref = resolveAt(p, 3);
        assertTrue(ref.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 4. comment
    // ----------------------------------------------------------------------
    @Test
    void comment_returnsEmpty() {
        String source = "// foo\nbar";
        Pipeline p = build(source);
        // Offset 2 is inside the line comment `// foo` ([0,6)).
        Optional<SymbolReference> ref = resolveAt(p, 2);
        assertTrue(ref.isEmpty(), "caret inside a comment must not produce a reference");
    }

    // ----------------------------------------------------------------------
    // 5. string literal
    // ----------------------------------------------------------------------
    @Test
    void stringLiteral_returnsEmpty() {
        String source = "String s = \"hello\";";
        Pipeline p = build(source);
        // The string `"hello"` occupies [11, 18). Offset 14 is inside.
        Optional<SymbolReference> ref = resolveAt(p, 14);
        assertTrue(ref.isEmpty(), "caret inside a string literal must not produce a reference");
    }

    // ----------------------------------------------------------------------
    // 6. character literal
    // ----------------------------------------------------------------------
    @Test
    void characterLiteral_returnsEmpty() {
        String source = "char c = 'a';";
        Pipeline p = build(source);
        // The char literal `'a'` occupies [9, 12). Offset 10 is inside.
        Optional<SymbolReference> ref = resolveAt(p, 10);
        assertTrue(ref.isEmpty(), "caret inside a character literal must not produce a reference");
    }

    // ----------------------------------------------------------------------
    // 7. offset before document
    // ----------------------------------------------------------------------
    @Test
    void offsetBeforeDocument_returnsEmpty() {
        String source = "int value = 10;";
        Pipeline p = build(source);
        Optional<SymbolReference> ref = resolveAt(p, -1);
        assertTrue(ref.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 8. offset after document
    // ----------------------------------------------------------------------
    @Test
    void offsetAfterDocument_returnsEmpty() {
        String source = "int value = 10;";
        Pipeline p = build(source);
        Optional<SymbolReference> ref = resolveAt(p, source.length() + 5);
        assertTrue(ref.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 9. offset at the end of an identifier
    // ----------------------------------------------------------------------
    @Test
    void offsetAtEndOfIdentifier() {
        String source = "int value = 10;";
        Pipeline p = build(source);
        // `value` is [4, 9). Offset 9 == endOffset — inclusive
        // canonical convention resolves the identifier.
        Optional<SymbolReference> ref = resolveAt(p, 9);
        assertTrue(ref.isPresent(), "end-of-identifier offset must still resolve");
        SymbolReference r = ref.get();
        assertEquals("value", r.name());
        assertEquals(TextRange.of(4, 9), r.range());
    }

    // ----------------------------------------------------------------------
    // 10. Unicode identifier
    // ----------------------------------------------------------------------
    @Test
    void unicodeIdentifier() {
        // Use a Java-letter Unicode identifier. Java accepts letters
        // from many scripts. JavaLexer treats the whole run as an
        // IDENTIFIER token.
        String source = "int \u00e9toile = 1;";
        Pipeline p = build(source);
        int idx = source.indexOf("\u00e9toile");
        Optional<SymbolReference> ref = resolveAt(p, idx);
        assertTrue(ref.isPresent());
        SymbolReference r = ref.get();
        assertEquals("\u00e9toile", r.name());
        assertEquals(SymbolReferenceKind.SIMPLE, r.kind());
    }

    // ----------------------------------------------------------------------
    // 11. qualified name (2 components)
    // ----------------------------------------------------------------------
    @Test
    void qualifiedNameTwoComponents() {
        String source = "foo.bar";
        Pipeline p = build(source);
        // `foo` at [0,3), `.` [3,4), `bar` [4,7). Offset 5 is inside
        // `bar`.
        Optional<SymbolReference> ref = resolveAt(p, 5);
        assertTrue(ref.isPresent());
        SymbolReference r = ref.get();
        assertEquals("foo.bar", r.name());
        assertEquals(TextRange.of(0, 7), r.range());
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, r.kind());
    }

    // ----------------------------------------------------------------------
    // 12. qualified name (3 components)
    // ----------------------------------------------------------------------
    @Test
    void qualifiedNameThreeComponents() {
        String source = "foo.bar.baz";
        Pipeline p = build(source);
        // `foo` [0,3), `.` [3,4), `bar` [4,7), `.` [7,8), `baz` [8,11).
        // Offset 9 is inside `baz`.
        Optional<SymbolReference> ref = resolveAt(p, 9);
        assertTrue(ref.isPresent());
        SymbolReference r = ref.get();
        assertEquals("foo.bar.baz", r.name());
        assertEquals(TextRange.of(0, 11), r.range());
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, r.kind());
    }

    // ----------------------------------------------------------------------
    // 13. this.field
    // ----------------------------------------------------------------------
    @Test
    void thisField_yieldsSimpleField() {
        // `this` is a KEYWORD token (not IDENTIFIER), so the caret-driven
        // resolver does NOT assemble `this.field` as a qualified
        // reference. Clicking on `field` yields a SIMPLE reference to
        // `field` with the range covering just the identifier.
        String source = "this.field";
        Pipeline p = build(source);
        int fieldOffset = 7; // inside `field` ([5, 10))
        Optional<SymbolReference> ref = resolveAt(p, fieldOffset);
        assertTrue(ref.isPresent());
        SymbolReference r = ref.get();
        assertEquals("field", r.name());
        assertEquals(TextRange.of(5, 10), r.range());
        assertEquals(SymbolReferenceKind.SIMPLE, r.kind());
    }

    @Test
    void thisField_caretOnKeyword_returnsEmpty() {
        // Clicking on `this` (a KEYWORD) yields no reference — the
        // resolver only treats IDENTIFIER tokens as anchors.
        String source = "this.field";
        Pipeline p = build(source);
        Optional<SymbolReference> ref = resolveAt(p, 2);
        assertTrue(ref.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 14. nonexistent symbol still produces a reference (resolution is
    // downstream). We assert the reference is well-formed even if the
    // symbol cannot be resolved — that's the contract of THIS layer.
    // ----------------------------------------------------------------------
    @Test
    void nonexistentSymbol_stillProducesReference() {
        String source = "class Example { void test() { nonexistent.foo = 1; } }";
        Pipeline p = build(source);
        // Pick offset inside `foo` (after `nonexistent.`). The lexer
        // will produce IDENTIFIER `.` IDENTIFIER even when the leftmost
        // component is not a declared symbol.
        int dotIdx = source.indexOf(".foo");
        int fooIdx = dotIdx + 1 + 1; // inside `foo` ([dotIdx+1, dotIdx+4))
        Optional<SymbolReference> ref = resolveAt(p, fooIdx);
        assertTrue(ref.isPresent());
        SymbolReference r = ref.get();
        // We don't fabricate qualified names; the reference carries
        // the textual form `nonexistent.foo` with a QUALIFIED_NAME
        // kind so downstream resolution can dispatch appropriately.
        assertEquals("nonexistent.foo", r.name());
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, r.kind());
    }

    // ----------------------------------------------------------------------
    // 15. exact ranges
    // ----------------------------------------------------------------------
    @Test
    void exactRanges_qualified() {
        String source = "foo.bar.baz";
        Pipeline p = build(source);
        // Caret on `foo` -> range [0,11)
        assertEquals(TextRange.of(0, 11), resolveAt(p, 0).orElseThrow().range());
        // Caret on `bar` -> range [0,11)
        assertEquals(TextRange.of(0, 11), resolveAt(p, 4).orElseThrow().range());
        // Caret on `baz` -> range [0,11)
        assertEquals(TextRange.of(0, 11), resolveAt(p, 8).orElseThrow().range());
    }

    @Test
    void exactRanges_simple() {
        String source = "foo bar";
        Pipeline p = build(source);
        // `foo` at [0,3)
        assertEquals(TextRange.of(0, 3), resolveAt(p, 0).orElseThrow().range());
        // `bar` at [4,7)
        assertEquals(TextRange.of(4, 7), resolveAt(p, 4).orElseThrow().range());
    }

    // ----------------------------------------------------------------------
    // 16. repeated resolution is deterministic
    // ----------------------------------------------------------------------
    @Test
    void repeatedResolutionIsDeterministic() {
        String source = "foo.bar.baz";
        Pipeline p = build(source);
        DefinitionReferenceResolver resolver = new DefinitionReferenceResolver();
        SymbolReference a = resolver.resolve(source, 5, p.table).orElseThrow();
        SymbolReference b = resolver.resolve(source, 5, p.table).orElseThrow();
        SymbolReference c = resolver.resolve(source, 5, p.table).orElseThrow();
        assertEquals(a.name(), b.name());
        assertEquals(a.name(), c.name());
        assertEquals(a.range(), b.range());
        assertEquals(a.range(), c.range());
        assertEquals(a.kind(), b.kind());
    }

    // ----------------------------------------------------------------------
    // Defensive: SymbolTable is not mutated
    // ----------------------------------------------------------------------
    @Test
    void symbolTableNotMutated() {
        String source = "int value = 10; value++;";
        Pipeline p = build(source);
        // Snapshot a structural signature (we don't have an
        // access-level helper here — compare root scope id + refRange
        // before and after).
        long rootIdBefore = p.table.rootScope().id();
        long rootChildrenBefore = p.table.rootScope().children().size();

        DefinitionReferenceResolver resolver = new DefinitionReferenceResolver();
        for (int offset : new int[] {4, 5, 6, 7, 8, 9, 16, 17, 18}) {
            resolver.resolve(source, offset, p.table);
        }

        long rootIdAfter = p.table.rootScope().id();
        long rootChildrenAfter = p.table.rootScope().children().size();
        assertEquals(rootIdBefore, rootIdAfter);
        assertEquals(rootChildrenBefore, rootChildrenAfter);
        assertFalse(rootIdBefore != rootIdAfter);
    }
}
