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

            @Override
            public void visitBlock(AstNode n) {
                counts.merge(AstNodeKind.BLOCK, 1, Integer::sum);
            }

            @Override
            public void visitEmptyStatement(AstNode n) {
                counts.merge(AstNodeKind.EMPTY_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitLocalVariableDeclaration(AstNode n) {
                counts.merge(AstNodeKind.LOCAL_VARIABLE_DECLARATION, 1, Integer::sum);
            }

            @Override
            public void visitExpressionStatement(AstNode n) {
                counts.merge(AstNodeKind.EXPRESSION_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitIfStatement(AstNode n) {
                counts.merge(AstNodeKind.IF_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitCondition(AstNode n) {
                counts.merge(AstNodeKind.CONDITION, 1, Integer::sum);
            }

            @Override
            public void visitThen(AstNode n) {
                counts.merge(AstNodeKind.THEN, 1, Integer::sum);
            }

            @Override
            public void visitElse(AstNode n) {
                counts.merge(AstNodeKind.ELSE, 1, Integer::sum);
            }

            @Override
            public void visitForStatement(AstNode n) {
                counts.merge(AstNodeKind.FOR_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitInitializer(AstNode n) {
                counts.merge(AstNodeKind.INITIALIZER, 1, Integer::sum);
            }

            @Override
            public void visitUpdate(AstNode n) {
                counts.merge(AstNodeKind.UPDATE, 1, Integer::sum);
            }

            @Override
            public void visitEnhancedForStatement(AstNode n) {
                counts.merge(AstNodeKind.ENHANCED_FOR_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitVariable(AstNode n) {
                counts.merge(AstNodeKind.VARIABLE, 1, Integer::sum);
            }

            @Override
            public void visitIterable(AstNode n) {
                counts.merge(AstNodeKind.ITERABLE, 1, Integer::sum);
            }

            @Override
            public void visitWhileStatement(AstNode n) {
                counts.merge(AstNodeKind.WHILE_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitDoWhileStatement(AstNode n) {
                counts.merge(AstNodeKind.DO_WHILE_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitReturnStatement(AstNode n) {
                counts.merge(AstNodeKind.RETURN_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitBreakStatement(AstNode n) {
                counts.merge(AstNodeKind.BREAK_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitContinueStatement(AstNode n) {
                counts.merge(AstNodeKind.CONTINUE_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitThrowStatement(AstNode n) {
                counts.merge(AstNodeKind.THROW_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitTryStatement(AstNode n) {
                counts.merge(AstNodeKind.TRY_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitCatchClause(AstNode n) {
                counts.merge(AstNodeKind.CATCH_CLAUSE, 1, Integer::sum);
            }

            @Override
            public void visitFinallyClause(AstNode n) {
                counts.merge(AstNodeKind.FINALLY_CLAUSE, 1, Integer::sum);
            }

            @Override
            public void visitSwitchStatement(AstNode n) {
                counts.merge(AstNodeKind.SWITCH_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitSwitchCase(AstNode n) {
                counts.merge(AstNodeKind.SWITCH_CASE, 1, Integer::sum);
            }

            @Override
            public void visitSwitchLabel(AstNode n) {
                counts.merge(AstNodeKind.SWITCH_LABEL, 1, Integer::sum);
            }

            @Override
            public void visitSynchronizedStatement(AstNode n) {
                counts.merge(AstNodeKind.SYNCHRONIZED_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitLabeledStatement(AstNode n) {
                counts.merge(AstNodeKind.LABELED_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitDeclarator(AstNode n) {
                counts.merge(AstNodeKind.DECLARATOR, 1, Integer::sum);
            }

            @Override
            public void visitSkipped(AstNode n) {
                counts.merge(AstNodeKind.SKIPPED, 1, Integer::sum);
            }

            @Override
            public void visitNameExpression(AstNode n) {
                counts.merge(AstNodeKind.NAME_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitThisExpression(AstNode n) {
                counts.merge(AstNodeKind.THIS_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitSuperExpression(AstNode n) {
                counts.merge(AstNodeKind.SUPER_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitLiteralExpression(AstNode n) {
                counts.merge(AstNodeKind.LITERAL_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitBinaryExpression(AstNode n) {
                counts.merge(AstNodeKind.BINARY_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitUnaryExpression(AstNode n) {
                counts.merge(AstNodeKind.UNARY_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitAssignmentExpression(AstNode n) {
                counts.merge(AstNodeKind.ASSIGNMENT_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitTernaryExpression(AstNode n) {
                counts.merge(AstNodeKind.TERNARY_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitInstanceofExpression(AstNode n) {
                counts.merge(AstNodeKind.INSTANCEOF_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitCastExpression(AstNode n) {
                counts.merge(AstNodeKind.CAST_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitMethodCallExpression(AstNode n) {
                counts.merge(AstNodeKind.METHOD_CALL_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitFieldAccessExpression(AstNode n) {
                counts.merge(AstNodeKind.FIELD_ACCESS_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitMethodReferenceExpression(AstNode n) {
                counts.merge(AstNodeKind.METHOD_REFERENCE_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitParenthesizedExpression(AstNode n) {
                counts.merge(AstNodeKind.PARENTHESIZED_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitObjectCreationExpression(AstNode n) {
                counts.merge(AstNodeKind.OBJECT_CREATION_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitArrayCreationExpression(AstNode n) {
                counts.merge(AstNodeKind.ARRAY_CREATION_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitClassLiteralExpression(AstNode n) {
                counts.merge(AstNodeKind.CLASS_LITERAL_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitLambdaExpression(AstNode n) {
                counts.merge(AstNodeKind.LAMBDA_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitArrayAccessExpression(AstNode n) {
                counts.merge(AstNodeKind.ARRAY_ACCESS_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitSwitchExpression(AstNode n) {
                counts.merge(AstNodeKind.SWITCH_EXPRESSION, 1, Integer::sum);
            }

            @Override
            public void visitYieldStatement(AstNode n) {
                counts.merge(AstNodeKind.YIELD_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitAssertStatement(AstNode n) {
                counts.merge(AstNodeKind.ASSERT_STATEMENT, 1, Integer::sum);
            }

            @Override
            public void visitTypePattern(AstNode n) {
                counts.merge(AstNodeKind.TYPE_PATTERN, 1, Integer::sum);
            }

            @Override
            public void visitOperator(AstNode n) {
                counts.merge(AstNodeKind.OPERATOR, 1, Integer::sum);
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
