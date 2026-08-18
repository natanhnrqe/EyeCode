package com.eyecode.language.semantic;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolKind;
import com.eyecode.language.symbol.SymbolReference;
import com.eyecode.language.symbol.SymbolReferenceKind;
import com.eyecode.language.symbol.SymbolScope;
import com.eyecode.language.symbol.SymbolTable;
import com.eyecode.language.symbol.SymbolTableBuilder;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MethodResolutionTest {

    private record Pipeline(SymbolTable table, String source) {}

    private Pipeline build(String source) {
        JavaLexerService lexer = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                lexer.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        JavaFileModel model = new JavaParser(stream).parse();
        SemanticModelSnapshot sem = new SymbolTableBuilder(model, 1, "Test.java", source).build();
        return new Pipeline(sem.symbolTable(), source);
    }

    private List<SymbolScope> allScopes(SymbolTable table) {
        List<SymbolScope> scopes = new ArrayList<>();
        collectScopes(table.rootScope(), scopes);
        return scopes;
    }

    private void collectScopes(SymbolScope scope, List<SymbolScope> scopes) {
        scopes.add(scope);
        for (SymbolScope child : scope.children()) {
            collectScopes(child, scopes);
        }
    }

    private Symbol findSymbol(SymbolTable table, String name, SymbolKind kind) {
        return allScopes(table).stream()
                .flatMap(scope -> table.symbolsIn(scope.id()).stream())
                .filter(symbol -> symbol.kind() == kind && symbol.name().equals(name))
                .findFirst()
                .orElseThrow();
    }

    @Test
    void unqualifiedMethodCall_resolvesWhenDeclaredLater() {
        String source = """
                class Example {
                    void run() {
                        calculate();
                        calculate();
                    }

                    void calculate() {}
                }
                """;
        Pipeline p = build(source);

        Symbol method = findSymbol(p.table, "calculate", SymbolKind.METHOD);
        List<SymbolReference> refs = p.table.referencesTo(method.id());

        assertEquals(2, refs.size());
        assertTrue(refs.stream().allMatch(ref -> ref.kind() == SymbolReferenceKind.SIMPLE));

        int callOffset = p.source.lastIndexOf("calculate");
        DefinitionLocation location = new DefinitionAtCaretResolver()
                .resolve(p.source, callOffset, p.table)
                .orElseThrow();
        assertEquals(method, location.symbol());
    }

    @Test
    void explicitThisMethodCall_resolvesWithoutDuplicateReferences() {
        String source = """
                class Example {
                    void calculate() {}

                    void run() {
                        this.calculate();
                    }
                }
                """;
        Pipeline p = build(source);

        Symbol method = findSymbol(p.table, "calculate", SymbolKind.METHOD);
        List<SymbolReference> refs = p.table.referencesTo(method.id());

        assertEquals(1, refs.size());
        assertEquals(SymbolReferenceKind.SIMPLE, refs.get(0).kind());
        assertEquals("calculate", refs.get(0).name());
        assertEquals(1, refs.stream().map(SymbolReference::range).distinct().count());
    }

    @Test
    void staticQualifiedMethodCall_resolvesAndIndexesExactRange() {
        String source = """
                class Utility {
                    static void calculate() {}
                }
                class Use {
                    void run() {
                        Utility.calculate();
                    }
                }
                """;
        Pipeline p = build(source);

        Symbol method = findSymbol(p.table, "calculate", SymbolKind.METHOD);
        List<SymbolReference> refs = p.table.referencesTo(method.id());

        assertEquals(1, refs.size());
        assertEquals(SymbolReferenceKind.QUALIFIED_NAME, refs.get(0).kind());
        assertEquals("Utility.calculate", refs.get(0).name());
        int start = p.source.indexOf("Utility.calculate");
        assertEquals(TextRange.of(start, start + "Utility.calculate".length()), refs.get(0).range());

        int caret = p.source.indexOf("Utility.calculate") + "Utility.".length();
        DefinitionLocation location = new DefinitionAtCaretResolver()
                .resolve(p.source, caret, p.table)
                .orElseThrow();
        assertEquals(method, location.symbol());
    }

    @Test
    void instanceQualifiedMethodCall_remainsUnresolved() {
        String source = """
                class Service {
                    void calculate() {}
                }
                class Use {
                    void run(Service service) {
                        service.calculate();
                    }
                }
                """;
        Pipeline p = build(source);

        Symbol method = findSymbol(p.table, "calculate", SymbolKind.METHOD);
        assertTrue(p.table.referencesTo(method.id()).isEmpty());

        int caret = p.source.indexOf("service.calculate") + "service.".length();
        assertTrue(new DefinitionAtCaretResolver().resolve(p.source, caret, p.table).isEmpty());
    }

    @Test
    void missingMethodDoesNotFabricateReference() {
        String source = """
                class Example {
                    void run() {
                        missing();
                    }
                }
                """;
        Pipeline p = build(source);

        int totalRefs = allScopes(p.table).stream()
                .flatMap(scope -> p.table.symbolsIn(scope.id()).stream())
                .mapToInt(symbol -> p.table.referencesTo(symbol.id()).size())
                .sum();
        assertEquals(0, totalRefs);
    }

    @Test
    void constructorCall_resolvesWhenExplicitConstructorExists() {
        String source = """
                class Foo {
                    Foo() {}
                }
                class Use {
                    void run() {
                        new Foo();
                    }
                }
                """;
        Pipeline p = build(source);

        Symbol constructor = findSymbol(p.table, "Foo", SymbolKind.CONSTRUCTOR);
        List<SymbolReference> refs = p.table.referencesTo(constructor.id());

        assertEquals(1, refs.size());
        assertEquals(SymbolReferenceKind.CONSTRUCTOR_CALL, refs.get(0).kind());

        int caret = p.source.indexOf("new Foo()") + "new ".length();
        DefinitionLocation location = new DefinitionAtCaretResolver()
                .resolve(p.source, caret, p.table)
                .orElseThrow();
        assertEquals(constructor, location.symbol());
    }

    @Test
    void constructorCallWithoutExplicitConstructorRemainsUnresolved() {
        String source = """
                class Foo {}
                class Use {
                    void run() {
                        new Foo();
                    }
                }
                """;
        Pipeline p = build(source);

        Symbol type = findSymbol(p.table, "Foo", SymbolKind.TYPE);
        assertEquals(1, p.table.referencesTo(type.id()).size());

        int caret = p.source.lastIndexOf("Foo");
        assertTrue(new DefinitionAtCaretResolver().resolve(p.source, caret, p.table).isEmpty());
    }
}
