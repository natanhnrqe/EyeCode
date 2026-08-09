package com.eyecode.language.ast;

import com.eyecode.editor.intelligence.document.TextRange;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AstVisitorTest {

    private static AstNode node(AstNodeKind kind) {
        return AstNode.of(kind, TextRange.of(0, 1), List.of());
    }

    @Test
    void visitDispatchesToPerKindMethod() {
        Map<AstNodeKind, Integer> counts = new EnumMap<>(AstNodeKind.class);
        AstVisitor visitor = new AstVisitor() {
            @Override
            public void visitCompilationUnit(AstNode n) {
                counts.merge(AstNodeKind.COMPILATION_UNIT, 1, Integer::sum);
            }

            @Override
            public void visitPackageDeclaration(AstNode n) {
                counts.merge(AstNodeKind.PACKAGE_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitImportDeclaration(AstNode n) {
                counts.merge(AstNodeKind.IMPORT_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitClassDeclaration(AstNode n) {
                counts.merge(AstNodeKind.CLASS_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitInterfaceDeclaration(AstNode n) {
                counts.merge(AstNodeKind.INTERFACE_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitEnumDeclaration(AstNode n) {
                counts.merge(AstNodeKind.ENUM_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitRecordDeclaration(AstNode n) {
                counts.merge(AstNodeKind.RECORD_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitAnnotation(AstNode n) {
                counts.merge(AstNodeKind.ANNOTATION, 1, Integer::sum);
            }

            @Override
            public void visitFieldDeclaration(AstNode n) {
                counts.merge(AstNodeKind.FIELD_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitMethodDeclaration(AstNode n) {
                counts.merge(AstNodeKind.METHOD_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitConstructorDeclaration(AstNode n) {
                counts.merge(AstNodeKind.CONSTRUCTOR_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitParameter(AstNode n) {
                counts.merge(AstNodeKind.PARAMETER, 1, Integer::sum);
            }

            @Override
            public void visitType(AstNode n) {
                counts.merge(AstNodeKind.TYPE, 1, Integer::sum);
            }

            @Override
            public void visitModifier(AstNode n) {
                counts.merge(AstNodeKind.MODIFIER, 1, Integer::sum);
            }
        };

        for (AstNodeKind kind : AstNodeKind.values()) {
            visitor.visit(node(kind));
        }

        assertEquals(AstNodeKind.values().length, counts.size());
        for (AstNodeKind kind : AstNodeKind.values()) {
            assertEquals(1, counts.get(kind), "visit() must dispatch to visit" + kind);
        }
    }

    @Test
    void traverseIsPreOrderDepthFirst() {
        AstNode d = node(AstNodeKind.FIELD_DECLARATION);
        AstNode e = node(AstNodeKind.FIELD_DECLARATION);
        AstNode b = AstNode.of(AstNodeKind.CLASS_DECLARATION, TextRange.of(1, 2), List.of(d, e));
        AstNode f = node(AstNodeKind.FIELD_DECLARATION);
        AstNode c = AstNode.of(AstNodeKind.CLASS_DECLARATION, TextRange.of(3, 4), List.of(f));
        AstNode a = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 5), List.of(b, c));

        List<AstNode> visited = new ArrayList<>();
        AstNodes.traverse(a, new AstVisitor() {
            @Override
            public void visit(AstNode node) {
                visited.add(node);
            }
        });

        assertEquals(List.of(a, b, d, e, c, f), visited);
    }

    @Test
    void traverseVisitsEachNodeExactlyOnce() {
        AstNode cu = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 30),
                List.of(
                        AstNode.of(AstNodeKind.CLASS_DECLARATION, TextRange.of(0, 20),
                                List.of(
                                        AstNode.of(AstNodeKind.FIELD_DECLARATION, TextRange.of(5, 9),
                                                List.of(AstNode.of(AstNodeKind.TYPE, TextRange.of(5, 8), List.of()))),
                                        AstNode.of(AstNodeKind.METHOD_DECLARATION, TextRange.of(10, 19),
                                                List.of(AstNode.of(AstNodeKind.TYPE, TextRange.of(10, 14), List.of()),
                                                        AstNode.of(AstNodeKind.PARAMETER, TextRange.of(15, 18),
                                                                List.of(AstNode.of(AstNodeKind.TYPE, TextRange.of(15, 18), List.of())))))))));

        List<AstNode> visited = new ArrayList<>();
        AstNodes.traverse(cu, new AstVisitor() {
            @Override
            public void visit(AstNode node) {
                visited.add(node);
            }
        });

        assertEquals(8, visited.size());
        assertEquals(visited.size(), visited.stream().distinct().count());
        assertEquals(AstNodeKind.COMPILATION_UNIT, visited.get(0).kind());
    }

    @Test
    void descendantsExcludesRoot() {
        AstNode type1 = node(AstNodeKind.TYPE);
        AstNode field = AstNode.of(AstNodeKind.FIELD_DECLARATION, TextRange.of(1, 2), List.of(type1));
        AstNode type2 = node(AstNodeKind.TYPE);
        AstNode method = AstNode.of(AstNodeKind.METHOD_DECLARATION, TextRange.of(3, 4), List.of(type2));
        AstNode cu = AstNode.of(AstNodeKind.COMPILATION_UNIT, TextRange.of(0, 5), List.of(field, method));

        List<AstNode> descendants = AstNodes.descendants(cu);

        assertEquals(4, descendants.size());
        assertEquals(List.of(field, type1, method, type2), descendants);
        assertFalse(descendants.contains(cu));
    }

    @Test
    void traversalRejectsNullArguments() {
        AstNode root = node(AstNodeKind.TYPE);
        assertThrows(IllegalArgumentException.class, () -> AstNodes.traverse(null, new AstVisitor() {
        }));
        assertThrows(IllegalArgumentException.class, () -> AstNodes.traverse(root, null));
    }
}
