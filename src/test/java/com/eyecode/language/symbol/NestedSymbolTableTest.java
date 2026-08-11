package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        // Find outer class
        Optional<Symbol> outer = table.lookup(table.rootScope().id(), "Outer");
        assertTrue(outer.isPresent());
        assertEquals(SymbolKind.TYPE, outer.get().kind());
        assertEquals(0, outer.get().ownerScopeId()); // Owned by root

        // Find inner class
        SymbolScope outerScope = snapshot.symbolTable().scope(outer.get().id().ownerScopeId()).orElseThrow();
        Optional<Symbol> inner = snapshot.symbolTable().findByName(outer.get().id(), "Inner");
        assertTrue(inner.isPresent());
        assertEquals(SymbolKind.TYPE, inner.get().kind());

        // Check owner relationships
        assertEquals(outer.get().id().ownerScopeId(), inner.get().ownerScopeId());

        // Check inner field
        SymbolScope innerScope = snapshot.symbolTable().scope(inner.get().id().ownerScopeId()).orElseThrow();
        Optional<Symbol> innerField = snapshot.symbolTable().findByName(inner.get().id(), "inner");
        assertTrue(innerField.isPresent());
        assertEquals(SymbolKind.FIELD, innerField.get().kind());
        assertEquals(inner.get().id().ownerScopeId(), innerField.get().ownerScopeId());

        // Check outer field accessible from inner
        Optional<Symbol> outerField = snapshot.symbolTable().lookup(inner.get().id().ownerScopeId(), "outer");
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

        SymbolScope aScope = snapshot.symbolTable().scope(a.get().id().ownerScopeId()).orElseThrow();
        Optional<Symbol> b = snapshot.symbolTable().findByName(a.get().id(), "B");
        assertTrue(b.isPresent());

        SymbolScope bScope = snapshot.symbolTable().scope(b.get().id().ownerScopeId()).orElseThrow();
        Optional<Symbol> c = snapshot.symbolTable().findByName(b.get().id(), "C");
        assertTrue(c.isPresent());

        // Check ownership chain
        assertEquals(a.get().id().ownerScopeId(), b.get().ownerScopeId());
        assertEquals(b.get().id().ownerScopeId(), c.get().ownerScopeId());
    }
}