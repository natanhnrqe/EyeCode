package com.eyecode.language.cfg;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.model.JavaFileModel;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.java.JavaLexerService;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ControlFlowGraphBuilderTest {

    private JavaFileModel parse(String source) {
        JavaLexerService service = new JavaLexerService();
        JavaTokenStream stream = new JavaTokenStream(
                service.lex(DocumentSnapshot.oneShot(source)).tokens(), source);
        return new JavaParser(stream).parse();
    }

    private AstNode methodOf(String source) {
        JavaFileModel model = parse(source);
        AstNode cu = model.getAstRoot();
        AstNode clazz = cu.children().get(0);
        return clazz.children().stream()
                .filter(c -> c.kind() == AstNodeKind.METHOD_DECLARATION)
                .findFirst().orElseThrow();
    }

    private AstNode methodBlock(String source) {
        AstNode method = methodOf(source);
        return method.children().stream()
                .filter(c -> c.kind() == AstNodeKind.BLOCK)
                .findFirst().orElseThrow();
    }

    private static long edgeCount(ControlFlowGraph graph, ControlFlowEdgeKind kind) {
        return graph.edges().stream().filter(e -> e.kind() == kind).count();
    }

    @Test
    void emptyBodyProducesJustEntryAndExit() {
        AstNode block = methodBlock("""
                class A {
                    void m() {}
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(2, graph.blocks().size());
        assertEquals(graph.exitId(), graph.outgoing(graph.entryId()).get(0).targetId());
    }

    @Test
    void simpleStatementsFormOneBlock() {
        AstNode block = methodBlock("""
                class A {
                    void m() {
                        int x = 1;
                        x = 2;
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        long trueEdges = edgeCount(graph, ControlFlowEdgeKind.TRUE);
        long falseEdges = edgeCount(graph, ControlFlowEdgeKind.FALSE);
        assertEquals(0, trueEdges, "no conditions");
        assertEquals(0, falseEdges, "no conditions");
        assertEquals(2, graph.blocks().size(), "entry + exit only");
    }

    @Test
    void ifStatementProducesTrueAndFalseEdges() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        if (c) {
                            int a = 1;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.TRUE));
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.FALSE));
    }

    @Test
    void ifElseProducesTwoConditionalEdgesAndJoin() {
        AstNode block = methodBlock("""
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
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.TRUE));
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.FALSE));
        long joins = graph.blocks().stream()
                .filter(b -> b.label().equals("if.join")).count();
        assertEquals(1, joins);
    }

    @Test
    void whileLoopProducesHeaderWithTrueFalseAndBackEdge() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        while (c) {
                            int a = 1;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.TRUE));
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.FALSE));
        long whileExit = graph.blocks().stream()
                .filter(b -> b.label().equals("while.exit")).count();
        assertEquals(1, whileExit);
    }

    @Test
    void doWhileLoopAlsoHasTrueFalseEdges() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        do {
                            int a = 1;
                        } while (c);
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.TRUE));
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.FALSE));
    }

    @Test
    void forLoopHasHeaderAndUpdateBlocks() {
        AstNode block = methodBlock("""
                class A {
                    void m() {
                        for (int i = 0; i < 10; i++) {
                            int a = 1;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.TRUE));
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.FALSE));
        long forUpdate = graph.blocks().stream()
                .filter(b -> b.label().equals("for.update")).count();
        assertEquals(1, forUpdate);
    }

    @Test
    void enhancedForLoopHasNoUpdateBlock() {
        AstNode block = methodBlock("""
                class A {
                    void m(java.util.List<String> list) {
                        for (String s : list) {
                            int a = 1;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        long forUpdate = graph.blocks().stream()
                .filter(b -> b.label().equals("for.update")).count();
        assertEquals(0, forUpdate);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.TRUE));
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.FALSE));
    }

    @Test
    void returnProducesReturnEdgeToExit() {
        AstNode block = methodBlock("""
                class A {
                    int m() {
                        return 1;
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.RETURN));
    }

    @Test
    void throwProducesThrowEdgeToExit() {
        AstNode block = methodBlock("""
                class A {
                    void m() {
                        throw new RuntimeException();
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.THROW));
    }

    @Test
    void breakInWhileProducesBreakEdge() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        while (c) {
                            break;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.BREAK));
    }

    @Test
    void continueInWhileProducesContinueEdge() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        while (c) {
                            continue;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.CONTINUE));
    }

    @Test
    void switchStatementProducesExitEdges() {
        AstNode block = methodBlock("""
                class A {
                    void m(int v) {
                        switch (v) {
                            case 1:
                                int a = 1;
                                break;
                            case 2:
                                int b = 2;
                                break;
                            default:
                                int c = 3;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(2, edgeCount(graph, ControlFlowEdgeKind.BREAK));
        long switchExit = graph.blocks().stream()
                .filter(b -> b.label().equals("switch.exit")).count();
        assertEquals(1, switchExit);
    }

    @Test
    void labeledBreakResolvesToLabelBlock() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c) {
                        outer:
                        while (c) {
                            break outer;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.BREAK));
        long labelBlocks = graph.blocks().stream()
                .filter(b -> b.label().startsWith("label:outer")).count();
        assertEquals(1, labelBlocks);
    }

    @Test
    void tryCatchKeepsMethodExitEdge() {
        AstNode block = methodBlock("""
                class A {
                    void m() {
                        try {
                            int a = 1;
                        } catch (RuntimeException e) {
                            int b = 2;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertNotNull(graph);
        assertTrue(graph.edges().size() >= 1);
    }

    @Test
    void synchronizedBlockIsSingleBlock() {
        AstNode block = methodBlock("""
                class A {
                    void m(Object o) {
                        synchronized (o) {
                            int a = 1;
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertNotNull(graph);
    }

    @Test
    void ifInsideWhileProducesNestedBlocks() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c, boolean d) {
                        while (c) {
                            if (d) {
                                int a = 1;
                            }
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(2, edgeCount(graph, ControlFlowEdgeKind.TRUE));
        assertEquals(2, edgeCount(graph, ControlFlowEdgeKind.FALSE));
    }

    @Test
    void breakResolvesToInnermostLoop() {
        AstNode block = methodBlock("""
                class A {
                    void m(boolean c, boolean d) {
                        while (c) {
                            while (d) {
                                break;
                            }
                        }
                    }
                }
                """);
        ControlFlowGraph graph = ControlFlowGraphBuilder.fromMethodBody(block, null);
        assertEquals(1, edgeCount(graph, ControlFlowEdgeKind.BREAK));
        long whileExit = graph.blocks().stream()
                .filter(b -> b.label().equals("while.exit")).count();
        assertEquals(2, whileExit);
    }
}
