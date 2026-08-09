package com.eyecode.language.cfg.analysis;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.cfg.ControlFlowGraph;
import com.eyecode.language.cfg.ControlFlowGraphBuilder;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlFlowAnalysesTest {

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

    private ControlFlowGraph build(String source) {
        return ControlFlowGraphBuilder.fromMethodBody(methodBlock(source), null);
    }

    @Test
    void emptyBodyHasNoUnreachableStatements() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {}
                }
                """);
        List<ControlFlowAnalyses.UnreachableStatement> unreachable =
                ControlFlowAnalyses.findUnreachable(graph);
        assertTrue(unreachable.isEmpty());
    }

    @Test
    void statementAfterReturnIsUnreachable() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {
                        return;
                        int x = 1;
                    }
                }
                """);
        List<ControlFlowAnalyses.UnreachableStatement> unreachable =
                ControlFlowAnalyses.findUnreachable(graph);
        assertEquals(1, unreachable.size());
        assertEquals(AstNodeKind.LOCAL_VARIABLE_DECLARATION, unreachable.get(0).kind());
    }

    @Test
    void statementAfterThrowIsUnreachable() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {
                        throw new RuntimeException();
                        int x = 1;
                    }
                }
                """);
        List<ControlFlowAnalyses.UnreachableStatement> unreachable =
                ControlFlowAnalyses.findUnreachable(graph);
        assertEquals(1, unreachable.size());
    }

    @Test
    void statementAfterBreakIsUnreachable() {
        ControlFlowGraph graph = build("""
                class A {
                    void m(boolean c) {
                        while (c) {
                            break;
                            int x = 1;
                        }
                    }
                }
                """);
        List<ControlFlowAnalyses.UnreachableStatement> unreachable =
                ControlFlowAnalyses.findUnreachable(graph);
        assertEquals(1, unreachable.size());
        assertEquals(AstNodeKind.LOCAL_VARIABLE_DECLARATION, unreachable.get(0).kind());
    }

    @Test
    void statementAfterContinueIsUnreachable() {
        ControlFlowGraph graph = build("""
                class A {
                    void m(boolean c) {
                        while (c) {
                            continue;
                            int x = 1;
                        }
                    }
                }
                """);
        List<ControlFlowAnalyses.UnreachableStatement> unreachable =
                ControlFlowAnalyses.findUnreachable(graph);
        assertEquals(1, unreachable.size());
    }

    @Test
    void normalStatementsAreReachable() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {
                        int a = 1;
                        int b = 2;
                        int c = 3;
                    }
                }
                """);
        List<ControlFlowAnalyses.UnreachableStatement> unreachable =
                ControlFlowAnalyses.findUnreachable(graph);
        assertTrue(unreachable.isEmpty());
    }

    @Test
    void ifStatementBranchesAreReachable() {
        ControlFlowGraph graph = build("""
                class A {
                    void m(boolean c) {
                        if (c) {
                            int a = 1;
                        } else {
                            int b = 2;
                        }
                    }
                }
                """);
        List<ControlFlowAnalyses.UnreachableStatement> unreachable =
                ControlFlowAnalyses.findUnreachable(graph);
        assertTrue(unreachable.isEmpty());
    }

    @Test
    void methodWithReturnEveryPathReturns() {
        ControlFlowGraph graph = build("""
                class A {
                    int m(boolean c) {
                        if (c) {
                            return 1;
                        } else {
                            return 2;
                        }
                    }
                }
                """);
        ControlFlowAnalyses.ReturnPathResult result =
                ControlFlowAnalyses.analyseReturnPaths(graph);
        assertTrue(result.anyPathReturns());
        assertTrue(result.allPathsReturn());
    }

    @Test
    void methodWithIfAndFallthroughDoesNotReturnAllPaths() {
        ControlFlowGraph graph = build("""
                class A {
                    int m(boolean c) {
                        if (c) {
                            return 1;
                        }
                        return 2;
                    }
                }
                """);
        ControlFlowAnalyses.ReturnPathResult result =
                ControlFlowAnalyses.analyseReturnPaths(graph);
        assertTrue(result.anyPathReturns());
        assertTrue(result.allPathsReturn());
    }

    @Test
    void voidMethodWithoutReturnHasNoReturnPaths() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {
                        int x = 1;
                    }
                }
                """);
        ControlFlowAnalyses.ReturnPathResult result =
                ControlFlowAnalyses.analyseReturnPaths(graph);
        assertFalse(result.anyPathReturns());
        assertFalse(result.allPathsReturn());
    }

    @Test
    void methodWithReturnAndFallthroughIsPartial() {
        ControlFlowGraph graph = build("""
                class A {
                    int m(boolean c) {
                        if (c) {
                            return 1;
                        }
                    }
                }
                """);
        ControlFlowAnalyses.ReturnPathResult result =
                ControlFlowAnalyses.analyseReturnPaths(graph);
        assertTrue(result.anyPathReturns());
        assertFalse(result.allPathsReturn());
    }

    @Test
    void reachableBlocksIncludesEntry() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {
                        int x = 1;
                    }
                }
                """);
        var reachable = ControlFlowAnalyses.reachableBlocks(graph);
        assertTrue(reachable.contains(graph.entryId()));
    }

    @Test
    void reachableBlocksExcludesUnreachableCode() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {
                        return;
                        int x = 1;
                    }
                }
                """);
        var reachable = ControlFlowAnalyses.reachableBlocks(graph);
        for (var entry : ControlFlowAnalyses.findUnreachable(graph)) {
            assertFalse(reachable.contains(entry.blockId()),
                    "block " + entry.blockId() + " should be unreachable");
        }
    }

    @Test
    void throwInMethodCountsAsTermination() {
        ControlFlowGraph graph = build("""
                class A {
                    void m() {
                        throw new RuntimeException();
                    }
                }
                """);
        ControlFlowAnalyses.ReturnPathResult result =
                ControlFlowAnalyses.analyseReturnPaths(graph);
        assertFalse(result.anyPathReturns(), "throw is not a return");
        assertTrue(result.pathsThatTerminate() > 0, "throw terminates the method");
    }
}
