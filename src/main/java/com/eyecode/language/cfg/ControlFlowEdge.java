package com.eyecode.language.cfg;

import java.util.Objects;

/**
 * Directed edge in a {@link ControlFlowGraph} (Sprint 5.3d).
 * <p>
 * An edge carries the {@link BasicBlock#id()} of its source and target
 * blocks and a {@link ControlFlowEdgeKind} describing how control flows
 * along it. Edges are immutable and identified by their (source, target,
 * kind) triple — multiple edges of distinct kinds between the same pair
 * of blocks are allowed (e.g. a conditional jumps both ways).
 * <p>
 * The synthetic entry and exit blocks of a graph have stable ids exposed
 * by {@link ControlFlowGraph#entryId()} and {@link ControlFlowGraph#exitId()}.
 */
public record ControlFlowEdge(int sourceId, int targetId, ControlFlowEdgeKind kind) {

    public ControlFlowEdge {
        if (sourceId < 0) {
            throw new IllegalArgumentException("sourceId < 0: " + sourceId);
        }
        if (targetId < 0) {
            throw new IllegalArgumentException("targetId < 0: " + targetId);
        }
        Objects.requireNonNull(kind, "kind must not be null");
    }

    @Override
    public String toString() {
        return sourceId + " -" + kind + "-> " + targetId;
    }
}
