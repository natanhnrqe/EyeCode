package com.eyecode.language.cfg.analysis;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.cfg.BasicBlock;
import com.eyecode.language.cfg.ControlFlowEdge;
import com.eyecode.language.cfg.ControlFlowEdgeKind;
import com.eyecode.language.cfg.ControlFlowGraph;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * First-pass analyses over a {@link ControlFlowGraph} (Sprint 5.3d).
 * <p>
 * Two analyses live here:
 * <ul>
 *   <li>{@link UnreachableStatementAnalysis} — collects statements whose
 *       enclosing basic block is unreachable from the entry block,</li>
 *   <li>{@link ReturnPathAnalysis} — reports whether the method
 *       represented by the graph has any path that returns a value and
 *       whether every path returns.</li>
 * </ul>
 * Both analyses run in linear time on the number of blocks plus edges and
 * are intentionally conservative: they do not reason about types,
 * constants, or partial evaluation — only structural reachability.
 */
public final class ControlFlowAnalyses {

    private ControlFlowAnalyses() {
    }

    /**
     * Computes the set of blocks reachable from the graph's entry block.
     */
    public static Set<Integer> reachableBlocks(ControlFlowGraph graph) {
        Set<Integer> reachable = new HashSet<>();
        Deque<Integer> worklist = new ArrayDeque<>();
        worklist.push(graph.entryId());
        reachable.add(graph.entryId());
        while (!worklist.isEmpty()) {
            int blockId = worklist.pop();
            for (ControlFlowEdge edge : graph.outgoing(blockId)) {
                if (reachable.add(edge.targetId())) {
                    worklist.push(edge.targetId());
                }
            }
        }
        return reachable;
    }

    /**
     * Result of the {@link UnreachableStatementAnalysis}.
     */
    public record UnreachableStatement(TextRange range, AstNodeKind kind, int blockId) {
    }

    /**
     * Detects statements that live in blocks unreachable from the entry
     * block. Statements in the synthetic entry/exit blocks are filtered
     * out — they are CFG plumbing, not real Java code.
     */
    public static List<UnreachableStatement> findUnreachable(ControlFlowGraph graph) {
        Set<Integer> reachable = reachableBlocks(graph);
        List<UnreachableStatement> result = new ArrayList<>();
        for (BasicBlock block : graph.blocks()) {
            if (block.id() == graph.entryId() || block.id() == graph.exitId()) {
                continue;
            }
            if (!reachable.contains(block.id())) {
                for (AstNode statement : block.statements()) {
                    result.add(new UnreachableStatement(statement.range(), statement.kind(), block.id()));
                }
            }
        }
        return result;
    }

    /**
     * Result of the {@link ReturnPathAnalysis}.
     * <p>
     * {@code pathsThatReturn} is the number of paths that reach the exit
     * via a {@link ControlFlowEdgeKind#RETURN} edge. {@code pathsThatTerminate}
     * counts every path that reaches the exit (return or throw). The two
     * extremes are the obvious diagnostics: {@code pathsThatReturn == 0}
     * → "missing return", {@code pathsThatReturn < pathsThatTerminate} →
     * "some paths don't return".
     */
    public record ReturnPathResult(boolean anyPathReturns, boolean allPathsReturn,
                                   long pathsThatReturn, long pathsThatTerminate) {
    }

    /**
     * Counts return paths through the graph. A "return path" is any
     * path that ends at the synthetic exit block via a {@link
     * ControlFlowEdgeKind#RETURN} edge. A "total path" is any simple
     * path from the entry block to the exit block; for control-flow
     * graphs with loops, paths are bounded by the loop nesting depth
     * to keep the analysis tractable (see {@link #PATH_BOUND}).
     */
    public static ReturnPathResult analyseReturnPaths(ControlFlowGraph graph) {
        PathCounter counter = new PathCounter(graph);
        counter.run();
        boolean anyReturn = counter.returns > 0;
        boolean allReturn = counter.terminations > 0
                && counter.returns == counter.terminations;
        return new ReturnPathResult(anyReturn, allReturn,
                counter.returns, counter.terminations);
    }

    /**
     * Upper bound on path enumeration depth to keep the analysis
     * tractable for cyclic graphs. Loops contribute one additional
     * iteration to the bound.
     */
    public static final int PATH_BOUND = 1_000_000;

    private static final class PathCounter {

        private final ControlFlowGraph graph;
        private final Set<Integer> visitedInPath = new HashSet<>();
        private long returns;
        private long terminations;
        private long explored;

        PathCounter(ControlFlowGraph graph) {
            this.graph = graph;
        }

        void run() {
            walk(graph.entryId(), 0);
        }

        private void walk(int blockId, int depth) {
            if (explored++ > PATH_BOUND) {
                return;
            }
            if (blockId == graph.exitId()) {
                terminations++;
                return;
            }
            if (depth > 0 && !visitedInPath.add(blockId)) {
                return;
            }
            try {
                List<ControlFlowEdge> outgoing = graph.outgoing(blockId);
                if (outgoing.isEmpty()) {
                    return;
                }
                for (ControlFlowEdge edge : outgoing) {
                    if (edge.kind() == ControlFlowEdgeKind.RETURN) {
                        returns++;
                        terminations++;
                    } else if (edge.kind() == ControlFlowEdgeKind.THROW) {
                        terminations++;
                    } else {
                        walk(edge.targetId(), depth + 1);
                    }
                }
            } finally {
                if (depth > 0) {
                    visitedInPath.remove(blockId);
                }
            }
        }
    }
}
