package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NestedSymbolTableTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private SymbolTableBuilder createBuilder(String source) {
        return new SymbolTableBuilder(parse(source), 1, "Test.java");
    }

    @Test
    void nestedClassOwnership() {
        String source = """
                class Outer {
                    int outer;
                    class Inner {
                        int inner;
                    }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> outer = table.lookup(table.rootScope().id(), "Outer");
        assertTrue(outer.isPresent());
        assertEquals(SymbolKind.TYPE, outer.get().kind());

        // Inner is declared inside Outer's type scope
        Optional<Symbol> inner = table.findByName(outer.get().scopeId(), "Inner");
        assertTrue(inner.isPresent());
        assertEquals(SymbolKind.TYPE, inner.get().kind());

        // Inner's ownerScopeId is Outer's type scope id (the scope where Inner is declared)
        assertEquals(outer.get().scopeId(), inner.get().ownerScopeId());

        // Inner's own field lives in Inner's scope
        Optional<Symbol> innerField = table.findByName(inner.get().scopeId(), "inner");
        assertTrue(innerField.isPresent());
        assertEquals(SymbolKind.FIELD, innerField.get().kind());

        // Outer's own field lives in Outer's scope
        Optional<Symbol> outerField = table.findByName(outer.get().scopeId(), "outer");
        assertTrue(outerField.isPresent());
        assertEquals(SymbolKind.FIELD, outerField.get().kind());
    }

    @Test
    void deeplyNestedTypes() {
        String source = """
                class A {
                    class B {
                        class C {
                            int x;
                        }
                    }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> a = table.lookup(table.rootScope().id(), "A");
        assertTrue(a.isPresent());
        Optional<Symbol> b = table.findByName(a.get().scopeId(), "B");
        assertTrue(b.isPresent());
        Optional<Symbol> c = table.findByName(b.get().scopeId(), "C");
        assertTrue(c.isPresent());

        // Lexical ownership chain
        assertEquals(a.get().scopeId(), b.get().ownerScopeId());
        assertEquals(b.get().scopeId(), c.get().ownerScopeId());

        // C's field x is in C's scope
        Optional<Symbol> x = table.findByName(c.get().scopeId(), "x");
        assertTrue(x.isPresent());
        assertEquals(SymbolKind.FIELD, x.get().kind());
    }
}
