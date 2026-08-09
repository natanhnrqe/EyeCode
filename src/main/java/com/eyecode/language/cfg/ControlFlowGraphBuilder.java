package com.eyecode.language.cfg;

import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Objects;

/**
 * Builds a {@link ControlFlowGraph} from an AST method body
 * (Sprint 5.3d).
 * <p>
 * Construction walks the AST in source order and produces a flat list of
 * basic blocks (numbered 0..n-1, with the synthetic entry at id 0 and
 * the synthetic exit at id 1) connected by directed
 * {@link ControlFlowEdge}s. Each {@link ControlFlowEdgeKind} corresponds
 * to a particular way control can transfer:
 * <ul>
 *   <li>{@link ControlFlowEdgeKind#NORMAL} — sequential fall-through,</li>
 *   <li>{@link ControlFlowEdgeKind#TRUE} / {@link ControlFlowEdgeKind#FALSE}
 *       — condition outcome,</li>
 *   <li>{@link ControlFlowEdgeKind#BREAK} / {@link ControlFlowEdgeKind#CONTINUE}
 *       — abrupt transfers out of / back to a loop or switch,</li>
 *   <li>{@link ControlFlowEdgeKind#RETURN} / {@link ControlFlowEdgeKind#THROW}
 *       — abrupt termination of the method.</li>
 * </ul>
 * Loop and switch constructs use a {@link BreakContext} stack: when the
 * builder enters a loop or switch, it pushes a context with the loop's
 * exit block (where {@code break} lands) and the loop's continue-target
 * block (where {@code continue} lands). When the builder leaves the
 * construct, the context is popped and any edges that were deferred to
 * it are resolved.
 * <p>
 * Labelled statements ({@code label: ...}) are tracked via
 * {@link LabelContext}: a {@code break label} / {@code continue label}
 * resolves the target through the label map.
 * <p>
 * <b>Out of scope (deferred to later sprints):</b>
 * <ul>
 *   <li>{@code try}/{@code catch}/{@code finally} — handled conservatively
 *       (the try block is laid out normally, catch/finally are skipped — the
 *       throw edge connects to the method exit),</li>
 *   <li>{@code synchronized} — modelled as a single block (no exception
 *       edges),</li>
 *   <li>throw inside try with matching catch (would need a catch block
 *       and a normal edge from throw into it),</li>
 *   <li>switch expression body construction (statements only for now),
 *   <li>cross-method call CFG (no inter-procedural edges).</li>
 * </ul>
 */
public final class ControlFlowGraphBuilder {

    private final List<AstNode> bodyStatements;
    private final AstNode methodNode;

    private final List<BasicBlock> blocks = new ArrayList<>();
    private final List<ControlFlowEdge> edges = new ArrayList<>();

    private final BasicBlock entry;
    private final BasicBlock exit;

    private BasicBlock current;
    private final Deque<BreakContext> breakStack = new ArrayDeque<>();
    private final Deque<LabelContext> labelStack = new ArrayDeque<>();

    public ControlFlowGraphBuilder(AstNode methodBody, AstNode methodNode) {
        Objects.requireNonNull(methodBody, "methodBody must not be null");
        this.methodNode = methodNode;
        this.bodyStatements = extractStatements(methodBody);
        this.entry = new BasicBlock(0, "entry");
        this.exit = new BasicBlock(1, "exit");
        blocks.add(entry);
        blocks.add(exit);
        this.current = entry;
    }

    public ControlFlowGraph build() {
        build(bodyStatements);
        sealCurrent();
        ensureEntryToExit();
        return new ControlFlowGraph(blocks, edges);
    }

    /**
     * Ensures that the synthetic entry block has a normal fall-through
     * edge to the exit block, which models "the method runs and returns
     * without entering any control-flow path" (used by empty bodies).
     * If the entry block already has outgoing edges, this is a no-op.
     */
    private void ensureEntryToExit() {
        for (ControlFlowEdge edge : edges) {
            if (edge.sourceId() == entry.id()) {
                return;
            }
        }
        addEdge(entry, exit, ControlFlowEdgeKind.NORMAL);
    }

    private static List<AstNode> extractStatements(AstNode methodBody) {
        if (methodBody.kind() == AstNodeKind.BLOCK) {
            return methodBody.children();
        }
        List<AstNode> single = new ArrayList<>();
        single.add(methodBody);
        return single;
    }

    private void build(List<AstNode> statements) {
        for (AstNode statement : statements) {
            buildStatement(statement);
        }
    }

    private void buildStatement(AstNode node) {
        if (node == null) {
            return;
        }
        if (current == exit) {
            return;
        }
        switch (node.kind()) {
            case BLOCK -> {
                List<AstNode> statements = node.children();
                for (AstNode child : statements) {
                    buildStatement(child);
                }
            }
            case LABELED_STATEMENT -> buildLabeledStatement(node);
            case IF_STATEMENT -> buildIf(node);
            case WHILE_STATEMENT -> buildWhile(node);
            case DO_WHILE_STATEMENT -> buildDoWhile(node);
            case FOR_STATEMENT -> buildFor(node);
            case ENHANCED_FOR_STATEMENT -> buildEnhancedFor(node);
            case SWITCH_STATEMENT -> buildSwitch(node, false);
            case SWITCH_EXPRESSION -> buildSwitch(node, true);
            case BREAK_STATEMENT -> buildBreak(node);
            case CONTINUE_STATEMENT -> buildContinue(node);
            case RETURN_STATEMENT -> buildReturn(node);
            case THROW_STATEMENT -> buildThrow(node);
            case TRY_STATEMENT -> buildTry(node);
            case SYNCHRONIZED_STATEMENT -> buildSynchronized(node);
            case LOCAL_VARIABLE_DECLARATION, EXPRESSION_STATEMENT,
                    EMPTY_STATEMENT, YIELD_STATEMENT, ASSERT_STATEMENT,
                    DECLARATOR, SKIPPED -> append(node);
            default -> append(node);
        }
    }

    private void buildLabeledStatement(AstNode node) {
        AstNode label = node.children().get(0);
        AstNode body = node.children().get(1);
        String labelName = labelText(label);
        BasicBlock labelBlock = openBlock("label:" + labelName);
        append(node);
        LabelContext context = new LabelContext(labelName, labelBlock);
        labelStack.push(context);
        try {
            buildStatement(body);
            BasicBlock bodyExit = current;
            sealAbruptTo(labelBlock, bodyExit);
        } finally {
            labelStack.pop();
        }
        current = labelBlock;
    }

    private static String labelText(AstNode label) {
        if (label.token() != null) {
            return label.token().text();
        }
        return label.children().isEmpty()
                ? ""
                : labelText(label.children().get(0));
    }

    private AstNode unwrap(AstNode node) {
        if (node == null) {
            return null;
        }
        AstNodeKind kind = node.kind();
        if (kind == AstNodeKind.THEN || kind == AstNodeKind.ELSE || kind == AstNodeKind.CONDITION
                || kind == AstNodeKind.INITIALIZER || kind == AstNodeKind.UPDATE
                || kind == AstNodeKind.VARIABLE || kind == AstNodeKind.ITERABLE
                || kind == AstNodeKind.SWITCH_LABEL) {
            if (node.children().isEmpty()) {
                return null;
            }
            return node.children().get(0);
        }
        return node;
    }

    private void buildIf(AstNode node) {
        List<AstNode> children = node.children();
        AstNode condition = unwrap(children.get(0));
        AstNode thenBranch = unwrap(children.get(1));
        AstNode elseBranch = children.size() >= 3 ? unwrap(children.get(2)) : null;

        append(condition);

        BasicBlock join = openBlock("if.join");
        BasicBlock thenBlock = openBlock("if.then");
        addEdge(current, thenBlock, ControlFlowEdgeKind.TRUE);

        BasicBlock falseTarget;
        if (elseBranch != null) {
            falseTarget = openBlock("if.else");
        } else {
            falseTarget = join;
        }
        addEdge(current, falseTarget, ControlFlowEdgeKind.FALSE);

        current = thenBlock;
        buildStatement(thenBranch);
        BasicBlock thenExit = current;
        sealAbruptTo(join, thenExit);
        current = join;

        if (elseBranch != null) {
            current = falseTarget;
            buildStatement(elseBranch);
            BasicBlock elseExit = current;
            sealAbruptTo(join, elseExit);
            current = join;
        }
    }

    private void buildWhile(AstNode node) {
        List<AstNode> children = node.children();
        AstNode condition = unwrap(children.get(0));
        AstNode body = unwrap(children.get(1));

        BasicBlock header = openBlock("while.header");
        BasicBlock exitBlock = openBlock("while.exit");
        addEdge(current, header, ControlFlowEdgeKind.NORMAL);
        BreakContext context = new BreakContext(BreakContext.Kind.LOOP, exitBlock, header, null);
        breakStack.push(context);
        try {
            current = header;
            append(condition);
            BasicBlock bodyBlock = openBlock("while.body");
            addEdge(current, bodyBlock, ControlFlowEdgeKind.TRUE);
            addEdge(current, exitBlock, ControlFlowEdgeKind.FALSE);
            current = bodyBlock;
            buildStatement(body);
            BasicBlock bodyExit = current;
            sealAbruptTo(header, bodyExit);
        } finally {
            breakStack.pop();
        }
        current = exitBlock;
    }

    private void sealAbruptTo(BasicBlock target, BasicBlock bodyExit) {
        if (bodyExit == exit) {
            return;
        }
        if (bodyExit.label().equals("unreachable")) {
            return;
        }
        if (bodyExit.statements().isEmpty()) {
            return;
        }
        if (isAbruptTerminator(lastKindOf(bodyExit))) {
            return;
        }
        if (!hasOutgoing(bodyExit.id())) {
            addEdge(bodyExit, target, ControlFlowEdgeKind.NORMAL);
        }
    }

    private void buildDoWhile(AstNode node) {
        List<AstNode> children = node.children();
        AstNode body = unwrap(children.get(0));
        AstNode condition = unwrap(children.get(1));

        BasicBlock bodyBlock = openBlock("do.body");
        BasicBlock header = openBlock("do.header");
        BasicBlock exitBlock = openBlock("do.exit");
        addEdge(current, bodyBlock, ControlFlowEdgeKind.NORMAL);
        BreakContext context = new BreakContext(BreakContext.Kind.LOOP, exitBlock, header, null);
        breakStack.push(context);
        try {
            current = bodyBlock;
            buildStatement(body);
            BasicBlock bodyExit = current;
            sealAbruptTo(header, bodyExit);
            current = header;
            append(condition);
            addEdge(current, bodyBlock, ControlFlowEdgeKind.TRUE);
            addEdge(current, exitBlock, ControlFlowEdgeKind.FALSE);
        } finally {
            breakStack.pop();
        }
        current = exitBlock;
    }

    private void buildFor(AstNode node) {
        List<AstNode> children = node.children();
        int idx = 0;
        AstNode init = null;
        AstNode condition = null;
        AstNode update = null;
        for (AstNode child : children) {
            AstNodeKind kind = child.kind();
            if (kind == AstNodeKind.INITIALIZER) {
                init = unwrap(child);
            } else if (kind == AstNodeKind.CONDITION) {
                condition = unwrap(child);
            } else if (kind == AstNodeKind.UPDATE) {
                update = unwrap(child);
            } else if (kind == AstNodeKind.THEN) {
                idx = children.indexOf(child);
                break;
            }
        }
        AstNode body = unwrap(children.get(idx));

        if (init != null) {
            append(init);
        }

        BasicBlock header = openBlock("for.header");
        BasicBlock exitBlock = openBlock("for.exit");
        addEdge(current, header, ControlFlowEdgeKind.NORMAL);
        BreakContext context = new BreakContext(BreakContext.Kind.LOOP, exitBlock, header, null);
        breakStack.push(context);
        try {
            current = header;
            if (condition != null) {
                append(condition);
            }
            BasicBlock bodyBlock = openBlock("for.body");
            BasicBlock updateBlock = update != null ? openBlock("for.update") : null;
            if (condition != null) {
                addEdge(current, bodyBlock, ControlFlowEdgeKind.TRUE);
                addEdge(current, exitBlock, ControlFlowEdgeKind.FALSE);
            } else {
                addEdge(current, bodyBlock, ControlFlowEdgeKind.NORMAL);
            }
            current = bodyBlock;
            buildStatement(body);
            BasicBlock bodyExit = current;
            BasicBlock backEdgeTarget = updateBlock != null ? updateBlock : header;
            sealAbruptTo(backEdgeTarget, bodyExit);
            if (updateBlock != null) {
                current = updateBlock;
                append(update);
                addEdge(current, header, ControlFlowEdgeKind.NORMAL);
            }
        } finally {
            breakStack.pop();
        }
        current = exitBlock;
    }

    private void buildEnhancedFor(AstNode node) {
        List<AstNode> children = node.children();
        AstNode variable = unwrap(children.get(0));
        AstNode iterable = unwrap(children.get(1));
        AstNode body = unwrap(children.get(2));

        append(variable);
        append(iterable);

        BasicBlock header = openBlock("foreach.header");
        BasicBlock exitBlock = openBlock("foreach.exit");
        addEdge(current, header, ControlFlowEdgeKind.NORMAL);
        BreakContext context = new BreakContext(BreakContext.Kind.LOOP, exitBlock, header, null);
        breakStack.push(context);
        try {
            current = header;
            BasicBlock bodyBlock = openBlock("foreach.body");
            addEdge(current, bodyBlock, ControlFlowEdgeKind.TRUE);
            addEdge(current, exitBlock, ControlFlowEdgeKind.FALSE);
            current = bodyBlock;
            buildStatement(body);
            BasicBlock bodyExit = current;
            sealAbruptTo(header, bodyExit);
        } finally {
            breakStack.pop();
        }
        current = exitBlock;
    }

    private void buildSwitch(AstNode node, boolean expression) {
        List<AstNode> children = node.children();
        AstNode selector = unwrap(children.get(0));

        append(selector);

        BasicBlock exitBlock = openBlock("switch.exit");
        BreakContext context = new BreakContext(BreakContext.Kind.SWITCH, exitBlock, null, null);
        breakStack.push(context);
        try {
            BasicBlock previous = current;
            int idx = 1;
            while (idx < children.size()) {
                AstNode switchCase = children.get(idx);
                List<AstNode> caseChildren = switchCase.children();
                AstNode label = caseChildren.get(0);
                boolean isDefault = label.children().isEmpty();
                BasicBlock caseBlock = openBlock(isDefault ? "switch.default" : "switch.case");
                addEdge(previous, caseBlock, isDefault
                        ? ControlFlowEdgeKind.NORMAL
                        : ControlFlowEdgeKind.TRUE);
                current = caseBlock;
                if (caseChildren.size() > 1) {
                    for (int i = 1; i < caseChildren.size(); i++) {
                        AstNode stmt = caseChildren.get(i);
                        buildStatement(stmt);
                        if (isAbruptTerminator(stmt.kind())) {
                            break;
                        }
                    }
                }
                BasicBlock caseExit = current;
                if (!isAbruptTerminator(lastKindOf(caseExit))
                        && caseExit != exitBlock
                        && !hasOutgoing(caseExit.id())) {
                    addEdge(caseExit, exitBlock, ControlFlowEdgeKind.NORMAL);
                }
                previous = caseBlock;
                idx++;
            }
        } finally {
            breakStack.pop();
        }
        current = exitBlock;
        if (expression) {
            append(node);
        }
    }

    private static AstNodeKind lastKindOf(BasicBlock block) {
        List<AstNode> statements = block.statements();
        if (statements.isEmpty()) {
            return null;
        }
        return statements.get(statements.size() - 1).kind();
    }

    private static boolean isAbruptTerminator(AstNodeKind kind) {
        if (kind == null) {
            return false;
        }
        return switch (kind) {
            case BREAK_STATEMENT, CONTINUE_STATEMENT, RETURN_STATEMENT,
                    THROW_STATEMENT, YIELD_STATEMENT -> true;
            default -> false;
        };
    }

    private void buildBreak(AstNode node) {
        append(node);
        AstNode labelChild = node.children().isEmpty() ? null : node.children().get(0);
        BasicBlock target = resolveBreakTarget(labelChild);
        addEdge(current, target, ControlFlowEdgeKind.BREAK);
        sealAbrupt();
    }

    private void buildContinue(AstNode node) {
        append(node);
        AstNode labelChild = node.children().isEmpty() ? null : node.children().get(0);
        BasicBlock target = resolveContinueTarget(labelChild);
        addEdge(current, target, ControlFlowEdgeKind.CONTINUE);
        sealAbrupt();
    }

    private BasicBlock resolveBreakTarget(AstNode labelChild) {
        String name = labelChild == null ? null : labelText(labelChild);
        if (name != null) {
            for (LabelContext ctx : labelStack) {
                if (ctx.name.equals(name)) {
                    return ctx.label;
                }
            }
        }
        for (BreakContext ctx : breakStack) {
            return ctx.exit;
        }
        return exit;
    }

    private BasicBlock resolveContinueTarget(AstNode labelChild) {
        String name = labelChild == null ? null : labelText(labelChild);
        if (name != null) {
            for (LabelContext ctx : labelStack) {
                if (ctx.name.equals(name)) {
                    return ctx.label;
                }
            }
        }
        for (BreakContext ctx : breakStack) {
            if (ctx.kind == BreakContext.Kind.LOOP && ctx.continueTarget != null) {
                return ctx.continueTarget;
            }
        }
        return exit;
    }

    private void buildReturn(AstNode node) {
        append(node);
        addEdge(current, exit, ControlFlowEdgeKind.RETURN);
        sealAbrupt();
    }

    private void buildThrow(AstNode node) {
        append(node);
        addEdge(current, exit, ControlFlowEdgeKind.THROW);
        sealAbrupt();
    }

    private void buildTry(AstNode node) {
        List<AstNode> children = node.children();
        AstNode tryBlock = children.get(0);
        if (tryBlock != null && tryBlock.kind() == AstNodeKind.BLOCK) {
            build(tryBlock.children());
        }
        boolean hasFinally = children.size() >= 2
                && children.get(children.size() - 1).kind() == AstNodeKind.FINALLY_CLAUSE;
        for (int i = 1; i < children.size(); i++) {
            AstNode clause = children.get(i);
            if (clause.kind() == AstNodeKind.FINALLY_CLAUSE) {
                continue;
            }
            append(clause);
            if (clause.children().size() >= 1) {
                AstNode clauseBody = clause.children().get(0);
                if (clauseBody != null && clauseBody.kind() == AstNodeKind.BLOCK) {
                    build(clauseBody.children());
                }
            }
        }
        if (hasFinally) {
            AstNode finallyClause = children.get(children.size() - 1);
            append(finallyClause);
            if (finallyClause.children().size() >= 1) {
                AstNode finallyBody = finallyClause.children().get(0);
                if (finallyBody != null && finallyBody.kind() == AstNodeKind.BLOCK) {
                    build(finallyBody.children());
                }
            }
        }
    }

    private void buildSynchronized(AstNode node) {
        append(node);
        if (node.children().size() >= 2) {
            AstNode body = unwrap(node.children().get(1));
            if (body != null && body.kind() == AstNodeKind.BLOCK) {
                build(body.children());
            }
        }
    }

    private void append(AstNode node) {
        if (node == null) {
            return;
        }
        if (current == exit) {
            return;
        }
        current.append(node);
    }

    private BasicBlock openBlock(String label) {
        BasicBlock block = new BasicBlock(blocks.size(), label);
        blocks.add(block);
        return block;
    }

    private void sealCurrent() {
        if (current == exit) {
            return;
        }
        if (current.label().equals("unreachable")) {
            return;
        }
        if (hasOutgoing(current.id())) {
            return;
        }
        if (!current.statements().isEmpty() && isAbruptTerminator(lastKindOf(current))) {
            return;
        }
        addEdge(current, exit, ControlFlowEdgeKind.NORMAL);
    }

    private boolean hasOutgoing(int blockId) {
        for (ControlFlowEdge edge : edges) {
            if (edge.sourceId() == blockId) {
                return true;
            }
        }
        return false;
    }

    private void sealAbrupt() {
        if (current == exit) {
            return;
        }
        BasicBlock next = new BasicBlock(blocks.size(), "unreachable");
        blocks.add(next);
        current = next;
    }

    private void addEdge(BasicBlock source, BasicBlock target, ControlFlowEdgeKind kind) {
        edges.add(new ControlFlowEdge(source.id(), target.id(), kind));
    }

    /**
     * Builds a CFG from the given method body AST node.
     * <p>
     * The body can be either a {@code BLOCK} node (typical — the body of a
     * method or constructor) or any single statement (treated as a
     * one-statement body). The {@code methodNode} is stored alongside the
     * graph for diagnostics but is not otherwise used in 5.3d.
     */
    public static ControlFlowGraph fromMethodBody(AstNode methodBody, AstNode methodNode) {
        return new ControlFlowGraphBuilder(methodBody, methodNode).build();
    }

    /**
     * Builds a CFG from a method or constructor declaration node. The
     * builder locates the method's {@code BLOCK} child and uses it as the
     * body.
     */
    public static ControlFlowGraph fromMethod(AstNode methodDeclaration) {
        AstNode body = null;
        for (AstNode child : methodDeclaration.children()) {
            if (child.kind() == AstNodeKind.BLOCK) {
                body = child;
                break;
            }
        }
        if (body == null) {
            return empty();
        }
        return fromMethodBody(body, methodDeclaration);
    }

    /**
     * Returns an empty CFG (just the entry→exit edge). Useful as a
     * fallback for abstract or empty method bodies.
     */
    public static ControlFlowGraph empty() {
        List<BasicBlock> blocks = new ArrayList<>();
        blocks.add(new BasicBlock(0, "entry"));
        blocks.add(new BasicBlock(1, "exit"));
        List<ControlFlowEdge> edges = new ArrayList<>();
        edges.add(new ControlFlowEdge(0, 1, ControlFlowEdgeKind.NORMAL));
        return new ControlFlowGraph(blocks, edges);
    }

    public AstNode methodNode() {
        return methodNode;
    }

    private BasicBlock exit() {
        return exit;
    }

    private record BreakContext(Kind kind, BasicBlock exit, BasicBlock continueTarget,
                                BasicBlock continueLabelTarget) {
        enum Kind { LOOP, SWITCH }
    }

    private record LabelContext(String name, BasicBlock label) {
    }
}
