package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaParserAstTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private static List<AstNode> ofKind(AstNode parent, AstNodeKind kind) {
        return parent.children().stream().filter(c -> c.kind() == kind).toList();
    }

    @Test
    void golden1_packageImportClassCompilationUnit() {
        JavaFileModel model = parse("package p;\nimport java.util.List;\nclass A {}\n");
        AstNode root = model.getAstRoot();

        assertEquals(AstNodeKind.COMPILATION_UNIT, root.kind());
        assertEquals(3, root.children().size());
        assertEquals(AstNodeKind.PACKAGE_DECLARATION, root.children().get(0).kind());
        assertEquals(AstNodeKind.IMPORT_DECLARATION, root.children().get(1).kind());
        assertEquals(AstNodeKind.CLASS_DECLARATION, root.children().get(2).kind());
    }

    @Test
    void golden2_multipleTopLevelTypes() {
        JavaFileModel model = parse("class A {}\nclass B {}\n");
        AstNode root = model.getAstRoot();

        List<AstNode> classes = ofKind(root, AstNodeKind.CLASS_DECLARATION);
        assertEquals(2, classes.size());
        assertEquals("A", model.getTypes().get(0).getName());
        assertEquals("B", model.getTypes().get(1).getName());
    }

    @Test
    void golden3_modifiersAndAnnotationsAreChildrenInSourceOrder() {
        JavaFileModel model = parse("@Deprecated\npublic final class A {}\n");
        AstNode clazz = model.getAstRoot().children().get(0);

        assertEquals(AstNodeKind.CLASS_DECLARATION, clazz.kind());
        assertEquals(3, clazz.children().size());
        assertEquals(AstNodeKind.ANNOTATION, clazz.children().get(0).kind());
        assertEquals(AstNodeKind.MODIFIER, clazz.children().get(1).kind());
        assertEquals(AstNodeKind.MODIFIER, clazz.children().get(2).kind());
    }

    @Test
    void golden4_fieldsWithTypeChildren() {
        String source = "class A { private int count; String name = \"x\"; }\n";
        JavaFileModel model = parse(source);
        AstNode clazz = model.getAstRoot().children().get(0);

        List<AstNode> fields = ofKind(clazz, AstNodeKind.FIELD_DECLARATION);
        assertEquals(2, fields.size());

        AstNode first = fields.get(0);
        assertEquals(2, first.children().size());
        assertEquals(AstNodeKind.MODIFIER, first.children().get(0).kind());
        assertEquals(AstNodeKind.TYPE, first.children().get(1).kind());
        assertEquals("private int count;",
                source.substring(first.range().startOffset(), first.range().endOffset()));

        AstNode second = fields.get(1);
        assertEquals(1, second.children().size());
        assertEquals(AstNodeKind.TYPE, second.children().get(0).kind());
        assertEquals("String",
                source.substring(second.children().get(0).range().startOffset(),
                        second.children().get(0).range().endOffset()));
    }

    @Test
    void golden5_methodsWithTypeAndParameterChildren() {
        JavaFileModel model = parse("""
                class A {
                    public int add(int a, int b) { return a + b; }
                    void run() {}
                }
                """);
        AstNode clazz = model.getAstRoot().children().get(0);

        List<AstNode> methods = ofKind(clazz, AstNodeKind.METHOD_DECLARATION);
        assertEquals(2, methods.size());

        AstNode add = methods.get(0);
        assertEquals(4, add.children().size());
        assertEquals(AstNodeKind.MODIFIER, add.children().get(0).kind());
        assertEquals(AstNodeKind.TYPE, add.children().get(1).kind());
        assertEquals(AstNodeKind.PARAMETER, add.children().get(2).kind());
        assertEquals(AstNodeKind.PARAMETER, add.children().get(3).kind());

        AstNode parameter = add.children().get(2);
        assertEquals(1, parameter.children().size());
        assertEquals(AstNodeKind.TYPE, parameter.children().get(0).kind());

        AstNode run = methods.get(1);
        assertEquals(1, run.children().size());
        assertEquals(AstNodeKind.TYPE, run.children().get(0).kind());
    }

    @Test
    void golden6_constructorWithAnnotationAndParameters() {
        JavaFileModel model = parse("""
                class A {
                    @Autowired
                    public A(String name, int id) {}
                }
                """);
        AstNode clazz = model.getAstRoot().children().get(0);

        List<AstNode> constructors = ofKind(clazz, AstNodeKind.CONSTRUCTOR_DECLARATION);
        assertEquals(1, constructors.size());

        AstNode constructor = constructors.get(0);
        assertEquals(4, constructor.children().size());
        assertEquals(AstNodeKind.ANNOTATION, constructor.children().get(0).kind());
        assertEquals(AstNodeKind.MODIFIER, constructor.children().get(1).kind());
        assertEquals(AstNodeKind.PARAMETER, constructor.children().get(2).kind());
        assertEquals(AstNodeKind.PARAMETER, constructor.children().get(3).kind());
        assertEquals(1, model.getTypes().get(0).getConstructors().size());
    }

    @Test
    void golden7_interfaceEnumAndRecordKinds() {
        JavaFileModel model = parse("""
                interface I {}
                enum Color { RED, GREEN }
                record Point(int x, int y) {}
                """);
        AstNode root = model.getAstRoot();

        assertEquals(3, root.children().size());
        assertEquals(AstNodeKind.INTERFACE_DECLARATION, root.children().get(0).kind());
        assertEquals(AstNodeKind.ENUM_DECLARATION, root.children().get(1).kind());
        assertEquals(AstNodeKind.RECORD_DECLARATION, root.children().get(2).kind());
    }

    @Test
    void golden8_nestedTypeIsChildOfEnclosingClass() {
        JavaFileModel model = parse("class Outer { enum Inner { A } }\n");
        AstNode clazz = model.getAstRoot().children().get(0);

        assertEquals(AstNodeKind.CLASS_DECLARATION, clazz.kind());
        assertEquals(1, clazz.children().size());
        assertEquals(AstNodeKind.ENUM_DECLARATION, clazz.children().get(0).kind());
        assertEquals(1, model.getTypes().get(0).getNestedTypes().size());
    }

    @Test
    void golden9_genericMethodHeaderIsTolerated() {
        JavaFileModel model = parse("""
                class A {
                    <T extends Number> T convert(T value) { return value; }
                }
                """);

        AstNode clazz = model.getAstRoot().children().get(0);
        List<AstNode> methods = ofKind(clazz, AstNodeKind.METHOD_DECLARATION);
        assertEquals(1, methods.size());
        assertEquals(AstNodeKind.METHOD_DECLARATION, methods.get(0).kind());
        assertEquals("T", model.getTypes().get(0).getMethods().get(0).getReturnType());
        assertEquals("T", model.getTypes().get(0).getMethods().get(0).getParameters().get(0).getType());
    }

    @Test
    void golden10_linkedTreeIsFullyNavigable() {
        JavaFileModel model = parse("""
                package p;
                class A {
                    private int x;
                    public int add(int a) { return x + a; }
                }
                """);
        AstNode root = model.getAstRoot();
        assertSame(root, model.getAstRoot());

        List<AstNode> all = AstNodes.descendants(root);
        assertFalse(all.isEmpty());

        for (AstNode node : all) {
            assertTrue(node.parent() != null, node + " must have a parent");
            assertTrue(node.parent().children().contains(node),
                    node + " must appear in its parent's children");
            assertTrue(root.range().contains(node.range()),
                    node + " must be inside the compilation unit");
        }
        assertNull(root.parent());
        assertSame(root, root.children().get(0).parent());
        assertEquals(2, model.getAstRoot().children().size());
        assertEquals(AstNodeKind.PACKAGE_DECLARATION, root.children().get(0).kind());
        assertEquals(AstNodeKind.CLASS_DECLARATION, root.children().get(1).kind());

        int fields = (int) all.stream().filter(n -> n.kind() == AstNodeKind.FIELD_DECLARATION).count();
        int methods = (int) all.stream().filter(n -> n.kind() == AstNodeKind.METHOD_DECLARATION).count();
        int types = (int) all.stream().filter(n -> n.kind() == AstNodeKind.TYPE).count();
        assertEquals(1, fields);
        assertEquals(1, methods);
        assertEquals(3, types);
    }
}
