package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Sprint 5.4c.2 — DefinitionAtCaretResolver facade tests.
 * <p>
 * End-to-end: caret offset → {@link DefinitionReferenceResolver} →
 * {@link SymbolReference} → {@link DefinitionResolver} →
 * {@link DefinitionLocation}. Validates the full spec §6.2 / §8
 * scenarios on top of real Java source.
 * <p>
 * Source-string-driven; offset positions derived from the lexer
 * diagnostic that preceded these tests (see
 * {@link DefinitionReferenceResolverTest} for the layout of
 * representative sources).
 */
class DefinitionAtCaretResolverTest {

    private record Pipeline(SymbolTable table, String source) {}

    private Pipeline build(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java").build();
        return new Pipeline(sem.symbolTable(), source);
    }

    private Optional<DefinitionLocation> resolveAt(Pipeline p, int offset) {
        return new DefinitionAtCaretResolver().resolve(p.source, offset, p.table);
    }

    // ----------------------------------------------------------------------
    // 1. local variable
    // ----------------------------------------------------------------------
    @Test
    void localVariable() {
        String source = """
                class Example {
                    void test() {
                        int local = 10;
                        int x = local;
                    }
                }
                """;
        Pipeline p = build(source);
        // Caret inside `local` on the RHS of `int x = local`.
        int useIdx = p.source.lastIndexOf("local");
        Optional<DefinitionLocation> loc = resolveAt(p, useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("local", l.symbol().name());
        assertEquals(SymbolKind.LOCAL_VARIABLE, l.symbol().kind());
    }

    // ----------------------------------------------------------------------
    // 2. parameter
    // ----------------------------------------------------------------------
    @Test
    void parameter() {
        String source = """
                class Example {
                    void test(int param) {
                        int x = param;
                    }
                }
                """;
        Pipeline p = build(source);
        int useIdx = p.source.lastIndexOf("param");
        Optional<DefinitionLocation> loc = resolveAt(p, useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("param", l.symbol().name());
        assertEquals(SymbolKind.PARAMETER, l.symbol().kind());
    }

    // ----------------------------------------------------------------------
    // 3. field
    // ----------------------------------------------------------------------
    @Test
    void field() {
        String source = """
                class Example {
                    int field;
                    void test() {
                        int x = field;
                    }
                }
                """;
        Pipeline p = build(source);
        int useIdx = p.source.lastIndexOf("field");
        Optional<DefinitionLocation> loc = resolveAt(p, useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("field", l.symbol().name());
        assertEquals(SymbolKind.FIELD, l.symbol().kind());
    }

    // ----------------------------------------------------------------------
    // 4. type
    // ----------------------------------------------------------------------
    @Test
    void type() {
        String source = """
                class MyClass {
                    int value;
                }
                class Use {
                    MyClass obj;
                }
                """;
        Pipeline p = build(source);
        // Caret inside `MyClass` of `MyClass obj`.
        int refIdx = p.source.indexOf("MyClass obj");
        Optional<DefinitionLocation> loc = resolveAt(p, refIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("MyClass", l.symbol().name());
        assertEquals(SymbolKind.TYPE, l.symbol().kind());
    }

    // ----------------------------------------------------------------------
    // 5. method
    // ----------------------------------------------------------------------
    @Test
    void method() {
        String source = """
                class Example {
                    int helper() { return 1; }
                    void test() {
                        int x = helper();
                    }
                }
                """;
        Pipeline p = build(source);
        // Caret inside the call `helper()` — `helper` is the second
        // textual occurrence (the first is the declaration).
        int callIdx = p.source.lastIndexOf("helper");
        Optional<DefinitionLocation> loc = resolveAt(p, callIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("helper", l.symbol().name());
        assertEquals(SymbolKind.METHOD, l.symbol().kind());
    }

    // ----------------------------------------------------------------------
    // 6. qualified reference
    // ----------------------------------------------------------------------
    @Test
    void qualifiedReference() {
        String source = """
                class MyClass {
                    static int value;
                }
                class Use {
                    void test() {
                        MyClass.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        // Caret inside `value` of `MyClass.value` — the resolver
        // assembles the whole `MyClass.value` qualified reference.
        int refIdx = p.source.indexOf("MyClass.value") + "MyClass.".length();
        Optional<DefinitionLocation> loc = resolveAt(p, refIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals("value", l.symbol().name());
        assertEquals(SymbolKind.FIELD, l.symbol().kind());
    }

    // ----------------------------------------------------------------------
    // 7. unresolved → empty
    // ----------------------------------------------------------------------
    @Test
    void unresolved_returnsEmpty() {
        String source = """
                class Example {
                    void test() {
                        int x = nonexistent;
                    }
                }
                """;
        Pipeline p = build(source);
        int refIdx = p.source.indexOf("nonexistent");
        Optional<DefinitionLocation> loc = resolveAt(p, refIdx);
        assertTrue(loc.isEmpty(),
                "unresolved reference must NOT fabricate a definition location");
    }

    // ----------------------------------------------------------------------
    // 8. whitespace → empty
    // ----------------------------------------------------------------------
    @Test
    void whitespace_returnsEmpty() {
        String source = "int    value = 10;";
        Pipeline p = build(source);
        // Pick an offset inside the multi-space whitespace region.
        int wsIdx = source.indexOf("   ") + 1;
        Optional<DefinitionLocation> loc = resolveAt(p, wsIdx);
        assertTrue(loc.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 9. comment → empty
    // ----------------------------------------------------------------------
    @Test
    void comment_returnsEmpty() {
        String source = """
                class Example {
                    // some comment
                    int field;
                }
                """;
        Pipeline p = build(source);
        int commentIdx = p.source.indexOf("// some");
        Optional<DefinitionLocation> loc = resolveAt(p, commentIdx + 1);
        assertTrue(loc.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 10. string literal → empty
    // ----------------------------------------------------------------------
    @Test
    void stringLiteral_returnsEmpty() {
        String source = """
                class Example {
                    String s = "hello world";
                    void test() {
                        int x = s.length();
                    }
                }
                """;
        Pipeline p = build(source);
        // Pick an offset inside the string literal `"hello world"`.
        int strIdx = p.source.indexOf("\"hello");
        int inside = strIdx + 5;
        Optional<DefinitionLocation> loc = resolveAt(p, inside);
        assertTrue(loc.isEmpty());
    }

    // ----------------------------------------------------------------------
    // 11. shadowing — innermost declaration wins
    // ----------------------------------------------------------------------
    @Test
    void shadowing_returnsInnermostDeclaration() {
        String source = """
                class Example {
                    int value;
                    void test(int value) {
                        int x = value;
                    }
                }
                """;
        Pipeline p = build(source);
        // Caret inside the RHS `value` (the parameter use). The
        // resolver must return the PARAMETER declaration (innermost),
        // not the FIELD declaration (outer).
        int useIdx = p.source.lastIndexOf("value");
        Optional<DefinitionLocation> loc = resolveAt(p, useIdx);
        assertTrue(loc.isPresent());
        DefinitionLocation l = loc.get();
        assertEquals(SymbolKind.PARAMETER, l.symbol().kind(),
                "innermost PARAMETER shadows outer FIELD");
    }

    // ----------------------------------------------------------------------
    // 12. SymbolTable is not mutated
    // ----------------------------------------------------------------------
    @Test
    void symbolTableNotMutated() {
        String source = """
                class Example {
                    int field;
                    void test(int param) {
                        int local = 10;
                        int a = local;
                        int b = field;
                        int c = param;
                        int d = nonexistent;
                        MyClass.value = 1;
                    }
                }
                """;
        Pipeline p = build(source);
        long rootIdBefore = p.table.rootScope().id();
        long rootChildrenBefore = p.table.rootScope().children().size();

        DefinitionAtCaretResolver resolver = new DefinitionAtCaretResolver();
        for (String token : new String[] {"local", "field", "param", "nonexistent"}) {
            int idx = p.source.lastIndexOf(token);
            if (idx >= 0) {
                resolver.resolve(p.source, idx, p.table);
            }
        }

        long rootIdAfter = p.table.rootScope().id();
        long rootChildrenAfter = p.table.rootScope().children().size();
        assertEquals(rootIdBefore, rootIdAfter);
        assertEquals(rootChildrenBefore, rootChildrenAfter);
        assertFalse(rootIdAfter != rootIdBefore);
    }
}
