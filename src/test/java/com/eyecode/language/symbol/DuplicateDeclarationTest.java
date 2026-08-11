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

class DuplicateDeclarationTest {

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
    void duplicateFieldInSameClass() {
        String source = """
                class A {
                    int x;
                    int x;
                }
                """;
        SymbolTableBuilder builder = createBuilder(source);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void duplicateMethodInSameClass() {
        String source = """
                class A {
                    void foo() {}
                    void foo() {}
                }
                """;
        SymbolTableBuilder builder = createBuilder(source);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void duplicateLocalVariableInSameMethod() {
        String source = """
                class A {
                    void m() {
                        int x = 1;
                        int x = 2;
                    }
                }
                """;
        SymbolTableBuilder builder = createBuilder(source);
        assertThrows(IllegalStateException.class, builder::build);
    }

    @Test
    void sameNameDifferentScopesAllowed() {
        String source = """
                class A {
                    int x;
                    void m() {
                        int x = 1;
                    }
                }
                """;
        SymbolTableBuilder builder = createBuilder(source);
        // Should not throw - same name in different scopes
        builder.build();
    }

    @Test
    void sameNameInDifferentClassesAllowed() {
        String source = """
                class A {
                    int x;
                }
                class B {
                    int x;
                }
                """;
        SymbolTableBuilder builder = createBuilder(source);
        // Should not throw - same name in different classes
        builder.build();
    }
}