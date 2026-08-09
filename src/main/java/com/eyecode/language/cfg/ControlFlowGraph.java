package com.eyecode.language.cfg;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Directed control-flow graph (Sprint 5.3d).
 * <p>
 * A CFG owns a flat sequence of {@link BasicBlock}s (numbered 0..n-1), a
 * synthetic entry block ({@link #entryId()} — id 0) and a synthetic exit
 * block ({@link #exitId()} — id 1), and a list of {@link ControlFlowEdge}s
 * between them.
 * <p>
 * The graph is built incrementally by the
 * {@link ControlFlowGraphBuilder}, which appends statements to the
 * current block, opens new blocks, and wires edges as control-flow
 * constructs are processed. After construction, the graph is immutable
 * and safe to query from multiple analyses.
 * <p>
 * Block ids are dense and stable: id {@code 0} is always the synthetic
 * entry, id {@code 1} the synthetic exit, and ids {@code >= 2} are
 * regular blocks created in construction order.
 */
public final class ControlFlowGraph {

    private final List<BasicBlock> blocks;
    private final List<ControlFlowEdge> edges;

    ControlFlowGraph(List<BasicBlock> blocks, List<ControlFlowEdge> edges) {
        this.blocks = List.copyOf(Objects.requireNonNull(blocks, "blocks must not be null"));
        this.edges = List.copyOf(Objects.requireNonNull(edges, "edges must not be null"));
    }

    public List<BasicBlock> blocks() {
        return blocks;
    }

    public List<ControlFlowEdge> edges() {
        return edges;
    }

    public BasicBlock entry() {
        return blocks.get(0);
    }

    public BasicBlock exit() {
        return blocks.get(1);
    }

    public int entryId() {
        return 0;
    }

    public int exitId() {
        return 1;
    }

    public BasicBlock block(int id) {
        return blocks.get(id);
    }

    /**
     * Returns the outgoing edges of the given block, in insertion order.
     */
    public List<ControlFlowEdge> outgoing(int blockId) {
        List<ControlFlowEdge> result = new ArrayList<>();
        for (ControlFlowEdge edge : edges) {
            if (edge.sourceId() == blockId) {
                result.add(edge);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns the incoming edges of the given block, in insertion order.
     */
    public List<ControlFlowEdge> incoming(int blockId) {
        List<ControlFlowEdge> result = new ArrayList<>();
        for (ControlFlowEdge edge : edges) {
            if (edge.targetId() == blockId) {
                result.add(edge);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Returns {@code true} when the given block has no incoming edges.
     * Useful for {@code unreachable} analyses (the synthetic entry block
     * always has incoming = none).
     */
    public boolean isOrphan(int blockId) {
        for (ControlFlowEdge edge : edges) {
            if (edge.targetId() == blockId) {
                return false;
            }
        }
        return true;
    }

    /**
     * Builds a CFG from an immutable collection of blocks and edges.
     * Validates that edge endpoints reference known block ids.
     */
    public static ControlFlowGraph of(List<BasicBlock> blocks, List<ControlFlowEdge> edges) {
        Map<Integer, BasicBlock> index = new LinkedHashMap<>();
        for (BasicBlock block : blocks) {
            if (index.put(block.id(), block) != null) {
                throw new IllegalArgumentException("duplicate block id: " + block.id());
            }
        }
        if (!index.containsKey(0) || !index.containsKey(1)) {
            throw new IllegalArgumentException("CFG must contain entry (0) and exit (1) blocks");
        }
        for (ControlFlowEdge edge : edges) {
            if (!index.containsKey(edge.sourceId())) {
                throw new IllegalArgumentException(
                        "edge source references unknown block: " + edge.sourceId());
            }
            if (!index.containsKey(edge.targetId())) {
                throw new IllegalArgumentException(
                        "edge target references unknown block: " + edge.targetId());
            }
        }
        return new ControlFlowGraph(new ArrayList<>(blocks), new ArrayList<>(edges));
    }

    @Override
    public String toString() {
        return "CFG[" + (blocks.size() - 2) + " blocks, " + edges.size() + " edges]";
    }

    /**
     * Returns the source range covered by the graph (the union of the
     * entry and exit blocks' ranges — entry is empty, so this is just
     * the exit block's range). Useful for tying the CFG back to the AST
     * root.
     */
    public TextRange range() {
        if (blocks.size() <= 2) {
            return TextRange.of(0, 0);
        }
        return exit().range();
    }
}
