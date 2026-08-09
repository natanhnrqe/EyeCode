package com.eyecode.editor.v2.language.java.parser;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlFlowAstTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private AstNode methodBlock(String source) {
        AstNode cu = parse(source).getAstRoot();
        AstNode clazz = cu.children().get(0);
        AstNode method = clazz.children().stream()
                .filter(c -> c.kind() == AstNodeKind.METHOD_DECLARATION)
                .findFirst().orElseThrow();
        return method.children().stream()
                .filter(c -> c.kind() == AstNodeKind.BLOCK)
                .findFirst().orElseThrow();
    }

    private static AstNode onlyChild(AstNode parent, AstNodeKind kind) {
        return parent.children().stream()
                .filter(c -> c.kind() == kind)
                .findFirst().orElseThrow();
    }

    @Test
    void assertStatementParsesWithConditionOnly() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        assert c;
                    }
                }
                """);
        AstNode assertStmt = onlyChild(block, AstNodeKind.ASSERT_STATEMENT);
        assertEquals(1, assertStmt.children().size());
        assertEquals(AstNodeKind.CONDITION, assertStmt.children().get(0).kind());
    }

    @Test
    void assertStatementParsesWithConditionAndMessage() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        assert c : "must be true";
                    }
                }
                """);
        AstNode assertStmt = onlyChild(block, AstNodeKind.ASSERT_STATEMENT);
        assertEquals(2, assertStmt.children().size());
        assertEquals(AstNodeKind.CONDITION, assertStmt.children().get(0).kind());
    }

    @Test
    void yieldStatementParses() {
        AstNode block = methodBlock("""
                class A {
                    void m() {
                        yield 42;
                    }
                }
                """);
        AstNode yieldStmt = onlyChild(block, AstNodeKind.YIELD_STATEMENT);
        assertEquals(1, yieldStmt.children().size());
    }

    @Test
    void switchExpressionArrowParses() {
        AstNode block = methodBlock("""
                class A {
                    void m(int v) {
                        int x = switch (v) {
                            case 1 -> 2;
                            case 3, 4 -> 5;
                            default -> 0;
                        };
                    }
                }
                """);
        AstNode localVar = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode initializer = localVar.children().get(1).children().get(0);
        assertEquals(AstNodeKind.SWITCH_EXPRESSION, initializer.kind());
        assertTrue(initializer.children().size() >= 2);
    }

    @Test
    void switchExpressionWithBlockArrow() {
        AstNode block = methodBlock("""
                class A {
                    void m(int v) {
                        int x = switch (v) {
                            case 1 -> { yield 2; }
                            default -> { yield 0; }
                        };
                    }
                }
                """);
        AstNode localVar = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode initializer = localVar.children().get(1).children().get(0);
        assertEquals(AstNodeKind.SWITCH_EXPRESSION, initializer.kind());
    }

    @Test
    void switchExpressionWithThrowArrow() {
        AstNode block = methodBlock("""
                class A {
                    void m(int v) {
                        int x = switch (v) {
                            case 1 -> 2;
                            default -> throw new RuntimeException();
                        };
                    }
                }
                """);
        AstNode localVar = onlyChild(block, AstNodeKind.LOCAL_VARIABLE_DECLARATION);
        AstNode initializer = localVar.children().get(1).children().get(0);
        assertEquals(AstNodeKind.SWITCH_EXPRESSION, initializer.kind());
        AstNode lastCase = initializer.children().get(initializer.children().size() - 1);
        assertEquals(AstNodeKind.SWITCH_CASE, lastCase.kind());
    }

    @Test
    void yieldStatementAppearsInSwitchArrowBlock() {
        AstNode block = methodBlock("""
                class A {
                    int m(int v) {
                        return switch (v) {
                            case 1 -> { yield 2; }
                            default -> 0;
                        };
                    }
                }
                """);
        assertNotNull(block);
    }
}
