package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolTableBuilderTest {

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
    void simpleClassWithField() {
        String source = """
                class A {
                    int field;
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        // Find the class symbol
        Optional<Symbol> classSymbol = table.lookup(table.rootScope().id(), "A");
        assertTrue(classSymbol.isPresent());
        assertEquals(SymbolKind.TYPE, classSymbol.get().kind());

        // Find the field
        SymbolTable table = snapshot.symbolTable();
        SymbolScope typeScope = table.findByName(table.rootScope().id(), "A")
                .flatMap(s -> table.scope(s.id()))
                .orElseThrow();
        Optional<Symbol> field = table.findByName(typeScope.id(), "field");
        assertTrue(field.isPresent());
        assertEquals(SymbolKind.FIELD, field.get().kind());
    }

    @Test
    void constructorWithParameters() {
        String source = """
                class A {
                    Foo(int value) {
                        int x = value;
                    }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        // Find the constructor
        SymbolScope classScope = table.findByName(table.rootScope().id(), "A")
                .flatMap(s -> table.scope(s.id()))
                .orElseThrow();
        Optional<Symbol> constructor = table.findByName(classScope.id(), "A");
        assertTrue(constructor.isPresent());
        assertEquals(SymbolKind.CONSTRUCTOR, constructor.get().kind());

        // Find parameter
        SymbolScope constructorScope = table.findByName(table.rootScope().id(), "A")
                .flatMap(s -> table.scope(s.id()))
                .flatMap(s -> table.findByName(s.id(), "A"))
                .flatMap(s -> table.scope(s.id()))
                .orElseThrow();
        // Actually the constructor is in the class scope
        Optional<Symbol> constructor = table.findByName(table.rootScope().id(), "A");
        assertTrue(constructor.isPresent());

        // Check that constructor has a parameter scope with 'value' parameter
        Symbol constructorSymbol = constructor.get();
        SymbolScope constructorScope = table.scope(constructorSymbol.id()).orElseThrow();
        Optional<Symbol> param = table.findByName(constructorScope.id(), "value");
        assertTrue(param.isPresent());
        assertEquals(SymbolKind.PARAMETER, param.get().kind());
    }

    @Test
    void methodWithParametersAndLocals() {
        String source = """
                class A {
                    void run(int parameter) {
                        int x = parameter;
                    }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> methodSymbol = table.lookup(table.rootScope().id(), "run");
        assertTrue(methodSymbol.isPresent());
        assertEquals(SymbolKind.METHOD, methodSymbol.get().kind());

        SymbolScope methodScope = table.scope(methodSymbol.get().id()).orElseThrow();
        Optional<Symbol> param = table.findByName(methodSymbol.id(), "parameter");
        assertTrue(param.isPresent());
        assertEquals(SymbolKind.PARAMETER, param.get().kind());

        Optional<Symbol> local = table.findByName(methodSymbol.id(), "x");
        assertTrue(local.isPresent());
        assertEquals(SymbolKind.LOCAL_VARIABLE, local.get().kind());
    }

    @Test
    void nestedClass() {
        String source = """
                class Outer {
                    int outerField;
                    class Inner {
                        int innerField;
                    }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        // Find outer class
        Optional<Symbol> outer = table.lookup(table.rootScope().id(), "Outer");
        assertTrue(outer.isPresent());
        assertEquals(SymbolKind.TYPE, outer.get().kind());

        // Find inner class
        SymbolScope outerScope = table.scope(outer.get().id()).orElseThrow();
        Optional<Symbol> inner = table.findByName(outer.get().id(), "Inner");
        assertTrue(inner.isPresent());
        assertEquals(SymbolKind.TYPE, inner.get().kind());

        // Check owner relationship
        Symbol innerSymbol = inner.get();
        assertEquals(outer.get().id(), innerSymbol.ownerScopeId());
    }

    @Test
    void methodWithReturnType() {
        String source = """
                class A {
                    int add(int a, int b) {
                        return a + b;
                    }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> methodSymbol = table.lookup(table.rootScope().id(), "add");
        assertTrue(methodSymbol.isPresent());
        assertEquals(SymbolKind.METHOD, methodSymbol.get().kind());
    }

    @Test
    void interfaceDeclaration() {
        String source = """
                interface Foo {
                    void bar();
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> interfaceSymbol = table.lookup(table.rootScope().id(), "Foo");
        assertTrue(interfaceSymbol.isPresent());
        assertEquals(SymbolKind.INTERFACE, interfaceSymbol.get().kind());
    }

    @Test
    void enumDeclaration() {
        String source = """
                enum Color {
                    RED, GREEN, BLUE
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> enumSymbol = table.lookup(table.rootScope().id(), "Color");
        assertTrue(enumSymbol.isPresent());
        assertEquals(SymbolKind.ENUM, enumSymbol.get().kind());
    }

    @Test
    void fieldWithInitializer() {
        String source = """
                class A {
                    int x = 1 + 2;
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> field = table.lookup(table.rootScope().id(), "x");
        assertTrue(field.isPresent());
        assertEquals(SymbolKind.FIELD, field.get().kind());
    }

    @Test
    void recordDeclaration() {
        String source = """
                record Point(int x, int y) {}
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> recordSymbol = table.lookup(table.rootScope().id(), "Point");
        assertTrue(recordSymbol.isPresent());
        assertEquals(SymbolKind.TYPE, recordSymbol.get().kind());
    }

    @Test
    void enumWithFields() {
        String source = """
                enum Color {
                    RED(1), GREEN(2), BLUE(3);
                    private int code;
                    Color(int code) { this.code = code; }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> enumSymbol = table.lookup(table.rootScope().id(), "Color");
        assertTrue(enumSymbol.isPresent());
        assertEquals(SymbolKind.ENUM, enumSymbol.get().kind());

        // Check enum constants and fields
        SymbolScope enumScope = table.scope(enumSymbol.get().id().ownerScopeId()).orElseThrow();
        Optional<Symbol> red = table.findByName(enumSymbol.get().id(), "RED");
        assertTrue(red.isPresent());
        Optional<Symbol> codeField = table.findByName(enumSymbol.get().id(), "code");
        assertTrue(codeField.isPresent());
        assertEquals(SymbolKind.FIELD, codeField.get().kind());
    }
}