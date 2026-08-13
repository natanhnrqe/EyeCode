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

        Optional<Symbol> classSymbol = table.lookup(table.rootScope().id(), "A");
        assertTrue(classSymbol.isPresent());
        assertEquals(SymbolKind.TYPE, classSymbol.get().kind());

        // The field is declared in the type's own scope (a TYPE child of root)
        Optional<Symbol> field = table.findByName(classSymbol.get().scopeId(), "field");
        assertTrue(field.isPresent());
        assertEquals(SymbolKind.FIELD, field.get().kind());
    }

    @Test
    void constructorWithParameters() {
        String source = """
                class A {
                    A(int value) {
                    }
                }
                """;
        SemanticModelSnapshot snapshot = createBuilder(source).build();
        SymbolTable table = snapshot.symbolTable();

        Optional<Symbol> typeSymbol = table.lookup(table.rootScope().id(), "A");
        assertTrue(typeSymbol.isPresent());
        assertEquals(SymbolKind.TYPE, typeSymbol.get().kind());

        // Constructor is declared in the type's scope under the same name "A"
        Optional<Symbol> constructor = table.findByName(typeSymbol.get().scopeId(), "A");
        assertTrue(constructor.isPresent());
        assertEquals(SymbolKind.CONSTRUCTOR, constructor.get().kind());

        // Parameters are declared in a BLOCK child of the constructor scope (constructor's
        // own "parameter scope"); SymbolScope.lookup walks up the parent chain (not children),
        // so we look for the parameter inside the constructor scope's BLOCK children.
        SymbolScope constructorScope = table.scope(constructor.get().scopeId()).orElseThrow();
        Optional<Symbol> param = Optional.empty();
        for (SymbolScope child : constructorScope.children()) {
            Optional<Symbol> p = child.findLocal("value");
            if (p.isPresent()) { param = p; break; }
        }
        // (constructor scope itself has no locals; param should be in the parameter BLOCK)
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

        Optional<Symbol> methodSymbol = table.findByName(
                table.lookup(table.rootScope().id(), "A").orElseThrow().scopeId(),
                "run");
        assertTrue(methodSymbol.isPresent());
        assertEquals(SymbolKind.METHOD, methodSymbol.get().kind());

        // Param/local symbols live in BLOCK children of the method scope; lookup walks them via children
        // via hierarchical chain — but lookup() walks *parents*, not children. So query the method scope
        // children for the BLOCK scopes.
        SymbolScope methodScope = table.scope(methodSymbol.get().scopeId()).orElseThrow();
        Optional<Symbol> param = methodScope.lookup("parameter");
        // Note: SymbolScope.lookup only walks parents, NOT children. params are in a child scope.
        // So this will be empty; instead, find via inspecting child scopes.
        if (param.isEmpty()) {
            for (SymbolScope child : methodScope.children()) {
                Optional<Symbol> p = child.lookup("parameter");
                if (p.isPresent()) {
                    param = p;
                    break;
                }
            }
        }
        assertTrue(param.isPresent());
        assertEquals(SymbolKind.PARAMETER, param.get().kind());

        Optional<Symbol> local = table.findByName(methodSymbol.get().scopeId(), "x");
        // Same situation. Look across the method scope's children for locals.
        if (local.isEmpty()) {
            for (SymbolScope child : methodScope.children()) {
                Optional<Symbol> l = child.findLocal("x");
                if (l.isPresent()) {
                    local = l;
                    break;
                }
            }
        }
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

        Optional<Symbol> outer = table.lookup(table.rootScope().id(), "Outer");
        assertTrue(outer.isPresent());
        assertEquals(SymbolKind.TYPE, outer.get().kind());

        Optional<Symbol> inner = table.findByName(outer.get().scopeId(), "Inner");
        assertTrue(inner.isPresent());
        assertEquals(SymbolKind.TYPE, inner.get().kind());

        // nested type's ownerScopeId == the enclosing type's scope id (where Inner is declared)
        assertEquals(outer.get().scopeId(), inner.get().ownerScopeId());
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

        Optional<Symbol> typeSymbol = table.lookup(table.rootScope().id(), "A");
        assertTrue(typeSymbol.isPresent());
        Optional<Symbol> methodSymbol = table.findByName(typeSymbol.get().scopeId(), "add");
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

        Optional<Symbol> a = table.lookup(table.rootScope().id(), "A");
        assertTrue(a.isPresent());
        Optional<Symbol> field = table.findByName(a.get().scopeId(), "x");
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

        // The code field is declared in the enum's scope
        Optional<Symbol> codeField = table.findByName(enumSymbol.get().scopeId(), "code");
        assertTrue(codeField.isPresent());
        assertEquals(SymbolKind.FIELD, codeField.get().kind());

        // NOTE: enum constants (RED, GREEN, BLUE) are NOT currently registered as symbols.
        // The parser's isField()/isConstructor() checks do not recognize the enum-constant
        // form `NAME(args)` so they fall through to `skipMember()` and become SKIPPED AST
        // nodes. This is a pre-existing limitation of the enum body parser, not a symbol
        // table bug; out of scope for Sprint 5.4b.1.
    }
}
