package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaClassModel;
import com.eyecode.editor.v2.language.java.model.JavaConstructorModel;
import com.eyecode.editor.v2.language.java.model.JavaFieldModel;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.model.JavaMethodModel;
import com.eyecode.editor.v2.language.java.model.JavaParameterModel;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AstRangeTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private static String slice(String source, TextRange range) {
        return source.substring(range.startOffset(), range.endOffset());
    }

    @Test
    void fileRangeCoversWholeCompilationUnit() {
        String source = "package p;\nimport java.util.List;\nclass A {}\n";
        JavaFileModel model = parse(source);

        assertEquals(TextRange.of(0, source.length()), model.getRange());
        assertEquals(model.getRange(), model.getAstRoot().range());
        assertEquals(AstNodeKind.COMPILATION_UNIT, model.getAstRoot().kind());
    }

    @Test
    void classRangeStartsAtFirstModifierOrAnnotation() {
        String source = "@Deprecated\npublic class Foo {}\n";
        JavaFileModel model = parse(source);
        JavaClassModel clazz = model.getTypes().get(0);

        assertEquals("@Deprecated\npublic class Foo {}", slice(source, clazz.getRange()));
    }

    @Test
    void classRangeWithoutModifiersStartsAtKindKeyword() {
        String source = "record Point(int x, int y) {}\n";
        JavaFileModel model = parse(source);
        JavaClassModel clazz = model.getTypes().get(0);

        assertEquals("record Point(int x, int y) {}", slice(source, clazz.getRange()));
    }

    @Test
    void methodRangeIncludesFullBody() {
        String source = """
                class A {
                    public int add(int a, int b) {
                        return a + b;
                    }
                }
                """;
        JavaFileModel model = parse(source);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);

        assertEquals("""
                        public int add(int a, int b) {
                                return a + b;
                            }""", slice(source, method.getRange()));
    }

    @Test
    void abstractMethodRangeEndsAtSemicolon() {
        String source = "interface I { void run(); }\n";
        JavaFileModel model = parse(source);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);

        assertEquals("void run();", slice(source, method.getRange()));
    }

    @Test
    void fieldRangeIncludesInitializer() {
        String source = "class A { private int count = 42; }\n";
        JavaFileModel model = parse(source);
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);

        assertEquals("private int count = 42;", slice(source, field.getRange()));
    }

    @Test
    void fieldRangeCoversMultipleDeclarators() {
        String source = "class A { int a = 1, b = 2; }\n";
        JavaFileModel model = parse(source);
        JavaFieldModel field = model.getTypes().get(0).getFields().get(0);

        assertEquals("int a = 1, b = 2;", slice(source, field.getRange()));
    }

    @Test
    void constructorRangeIncludesBody() {
        String source = "class A { public A() { } }\n";
        JavaFileModel model = parse(source);
        JavaConstructorModel constructor = model.getTypes().get(0).getConstructors().get(0);

        assertEquals("public A() { }", slice(source, constructor.getRange()));
    }

    @Test
    void parameterRangeCoversTypeAndName() {
        String source = "class A { void m(String name, int count) {} }\n";
        JavaFileModel model = parse(source);
        JavaMethodModel method = model.getTypes().get(0).getMethods().get(0);
        JavaParameterModel first = method.getParameters().get(0);
        JavaParameterModel second = method.getParameters().get(1);

        assertEquals("String name", slice(source, first.getRange()));
        assertEquals("int count", slice(source, second.getRange()));
    }

    @Test
    void nestedTypeRangeCoversDeclaration() {
        String source = "class Outer { enum Inner { A, B } }\n";
        JavaFileModel model = parse(source);
        JavaClassModel inner = model.getTypes().get(0).getNestedTypes().get(0);

        assertEquals("enum Inner { A, B }", slice(source, inner.getRange()));
    }
}
