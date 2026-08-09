package com.eyecode.language.cfg;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * A node in a {@link ControlFlowGraph} (Sprint 5.3d).
 * <p>
 * A basic block is a maximal straight-line sequence of statements that
 * always execute together (no internal jumps). It owns:
 * <ul>
 *   <li>an immutable, monotonically assigned {@code id} (used by edges
 *       and by analyses),</li>
 *   <li>the AST nodes that compose the block in source order,</li>
 *   <li>the {@link TextRange} spanning the first to last statement (or
 *       {@link TextRange#isEmpty()} for the synthetic entry/exit blocks),</li>
 *   <li>an optional label identifying its role (e.g. {@code "entry"},
 *       {@code "exit"}, or a loop header).</li>
 * </ul>
 * New blocks are created by {@link ControlFlowGraph#newBlock(String)};
 * nodes are appended via {@link #append(AstNode)} during CFG construction.
 */
public final class BasicBlock {

    private final int id;
    private final String label;
    private final List<AstNode> statements = new ArrayList<>();
    private TextRange range = TextRange.of(0, 0);

    BasicBlock(int id, String label) {
        this.id = id;
        this.label = label == null ? "" : label;
    }

    public int id() {
        return id;
    }

    public String label() {
        return label;
    }

    /**
     * Statements that compose the block, in source order. Empty for the
     * synthetic entry/exit blocks.
     */
    public List<AstNode> statements() {
        return Collections.unmodifiableList(statements);
    }

    public TextRange range() {
        return range;
    }

    /**
     * Appends a statement (or statement-like AST node) to the end of this
     * block, extending its source range to cover the appended node.
     */
    void append(AstNode node) {
        Objects.requireNonNull(node, "node must not be null");
        statements.add(node);
        TextRange nodeRange = node.range();
        if (statements.size() == 1) {
            range = nodeRange;
        } else {
            range = TextRange.of(Math.min(range.startOffset(), nodeRange.startOffset()),
                    Math.max(range.endOffset(), nodeRange.endOffset()));
        }
    }

    @Override
    public String toString() {
        return "BasicBlock[" + id + (label.isEmpty() ? "" : ":" + label) + "]";
    }
}
