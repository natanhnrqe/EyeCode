package com.eyecode.language.symbol;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SymbolTableBuilderModifierPopulationTest {

    @Test
    void staticFieldGetsStaticModifier() {
        assertFieldModifiers("class Constants { static int value; }", "value", Set.of(SymbolModifier.STATIC));
    }

    @Test
    void finalFieldGetsFinalModifier() {
        assertFieldModifiers("class Constants { final int value = 1; }", "value", Set.of(SymbolModifier.FINAL));
    }

    @Test
    void publicFieldGetsPublicModifier() {
        assertFieldModifiers("class Constants { public int value; }", "value", Set.of(SymbolModifier.PUBLIC));
    }

    @Test
    void privateFieldGetsPrivateModifier() {
        assertFieldModifiers("class Constants { private int value; }", "value", Set.of(SymbolModifier.PRIVATE));
    }

    @Test
    void protectedFieldGetsProtectedModifier() {
        assertFieldModifiers("class Constants { protected int value; }", "value", Set.of(SymbolModifier.PROTECTED));
    }

    @Test
    void multipleFieldModifiersArePreserved() {
        assertFieldModifiers(
                "class Constants { public static final int MAX = 10; }",
                "MAX",
                Set.of(SymbolModifier.PUBLIC, SymbolModifier.STATIC, SymbolModifier.FINAL));
    }

    @Test
    void fieldWithoutModifierHasEmptySet() {
        assertFieldModifiers("class Constants { int value; }", "value", Set.of());
    }

    @Test
    void staticAndFinalMethodModifiersArePreserved() {
        SymbolTable table = build("class Constants { public static final int value() { return 1; } }");
        Symbol method = find(table, "value", SymbolKind.METHOD);

        assertEquals(Set.of(SymbolModifier.PUBLIC, SymbolModifier.STATIC, SymbolModifier.FINAL), method.modifiers());
    }

    @Test
    void typeModifiersArePreserved() {
        SymbolTable table = build("public final class Constants { int value; }");
        Symbol type = find(table, "Constants", SymbolKind.TYPE);

        assertEquals(Set.of(SymbolModifier.PUBLIC, SymbolModifier.FINAL), type.modifiers());
    }

    @Test
    void constructorModifiersArePreservedWhenAvailable() {
        SymbolTable table = build("class Constants { public Constants() {} }");
        Symbol constructor = find(table, "Constants", SymbolKind.CONSTRUCTOR);

        assertEquals(Set.of(SymbolModifier.PUBLIC), constructor.modifiers());
    }

    @Test
    void modifierPopulationIsDeterministic() {
        String source = "class Constants { public static final int MAX = 10; }";

        assertEquals(
                find(build(source), "MAX", SymbolKind.FIELD).modifiers(),
                find(build(source), "MAX", SymbolKind.FIELD).modifiers());
    }

    @Test
    void snapshotPreservesPopulatedModifiers() {
        SymbolTable table = build("class Constants { public static final int MAX = 10; }");
        Symbol field = find(table, "MAX", SymbolKind.FIELD);

        assertEquals(Set.of(SymbolModifier.PUBLIC, SymbolModifier.STATIC, SymbolModifier.FINAL),
                table.find(field.id()).orElseThrow().modifiers());
    }

    private void assertFieldModifiers(String source, String name, Set<SymbolModifier> expected) {
        assertEquals(expected, find(build(source), name, SymbolKind.FIELD).modifiers());
    }

    private SymbolTable build(String source) {
        JavaFileModel model = parse(source);
        return new SymbolTableBuilder(model, 1, "Test.java", source).build().symbolTable();
    }

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private Symbol find(SymbolTable table, String name, SymbolKind kind) {
        return find(table.rootScope(), name, kind);
    }

    private Symbol find(SymbolScope scope, String name, SymbolKind kind) {
        Symbol local = scope.findLocal(name).filter(symbol -> symbol.kind() == kind).orElse(null);
        if (local != null) {
            return local;
        }
        for (SymbolScope child : scope.children()) {
            try {
                return find(child, name, kind);
            } catch (AssertionError ignored) {
            }
        }
        throw new AssertionError("Missing " + kind + " " + name);
    }
}
