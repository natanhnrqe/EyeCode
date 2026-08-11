package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Sprint54aSymbolIntegrationTest {

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
    void fullPipelineProducesSymbolTable() {
        String source = """
                class Foo {
                    void run() {
                        int x = 1;
                    }
                }
                """;
        JavaFileModel model = parse(source);
        SemanticModelSnapshot snapshot = new SymbolTableBuilder(model, 1, "Test.java").build();

        assertNotNull(snapshot);
        assertEquals(1, snapshot.version());
        assertEquals("Test.java", snapshot.sourceFile());
        assertNotNull(snapshot.symbolTable());
    }

    @Test
    void lookupFindsClass() {
        String source = """
                class Foo {
                    void run() {}
                }
                """;
        SemanticModelSnapshot snapshot = new SymbolTableBuilder(parse(source), 1, "Test.java").build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> foo = table.lookup(table.rootScope().id(), "Foo");
        assertTrue(foo.isPresent());
        assertEquals("Foo", foo.get().name());
        assertEquals(SymbolKind.TYPE, foo.get().kind());
    }

    @Test
    void lookupFindsMethod() {
        String source = """
                class Foo {
                    void run() {}
                }
                """;
        SemanticModelSnapshot snapshot = new SymbolTableBuilder(parse(source), 1, "Test.java").build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> foo = table.lookup(table.rootScope().id(), "Foo");
        assertTrue(foo.isPresent());
        SymbolScope fooScope = snapshot.symbolTable().scope(foo.get().id().ownerScopeId()).orElseThrow();
        Optional<Symbol> run = snapshot.symbolTable().findByName(foo.get().id(), "run");
        assertTrue(run.isPresent());
        assertEquals(SymbolKind.METHOD, run.get().kind());
    }

    @Test
    void lookupFindsField() {
        String source = """
                class Foo {
                    int field;
                }
                """;
        SemanticModelSnapshot snapshot = new SymbolTableBuilder(parse(source), 1, "Test.java").build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> foo = table.lookup(table.rootScope().id(), "Foo");
        assertTrue(foo.isPresent());
        SymbolScope fooScope = snapshot.symbolTable().scope(foo.get().id().ownerScopeId()).orElseThrow();
        Optional<Symbol> field = snapshot.symbolTable().findByName(foo.get().id(), "field");
        assertTrue(field.isPresent());
        assertEquals(SymbolKind.FIELD, field.get().kind());
    }

    @Test
    void snapshotVersionMatchesDocument() {
        String source = "class A {}";
        SemanticModelSnapshot snapshot = new SymbolTableBuilder(parse(source), 42, "Test.java").build();
        assertEquals(42, snapshot.version());
    }
}