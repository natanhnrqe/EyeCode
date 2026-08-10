package com.eyecode.language.java.parser.incremental;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.editor.v2.language.java.lexer.JavaTokenStream;
import com.eyecode.editor.v2.language.java.parser.JavaParser;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.java.JavaLexerService;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Conservative incremental parser.
 * <p>
 * Strategy: locate the smallest reparsable region for the change via
 * {@link ParserChangeAnalyzer}; if none is available, fall back to
 * {@link FullReparseStrategy}. When a region is available, reparse ONLY
 * the smallest enclosing statement (or block) and splice it into a clone
 * of the previous AST whose ranges are shifted by the change's delta.
 * <p>
 * The strategy never mutates nodes from the previous AST in place — it
 * produces a fresh tree built from {@link AstNode#of(AstNodeKind, TextRange,
 * List)} (or the token-bearing overload). Subtrees outside the reparsable
 * region are reconstructed with shifted ranges but identical structure, so
 * structural equivalence with a full reparse is preserved.
 *
 * <h2>Why conservative</h2>
 * The strategy refuses to do anything beyond shifting-and-cloning outside
 * the reparsable region. Editing across structural delimiters, between
 * sibling statements, or anywhere the analyzer flags as ambiguous forces a
 * full reparse. The cost is a slower first edit in those cases; the
 * benefit is a sound equivalence guarantee with the full parser.
 */
public final class IncrementalParserStrategy {

    private final FullReparseStrategy fallback = new FullReparseStrategy();
    private final JavaLexerService lexer = new JavaLexerService();

    public Result parse(DocumentSnapshot snapshot,
                        DocumentSnapshot previous,
                        AstNode previousRoot,
                        TextChange change) {
        Objects.requireNonNull(snapshot, "snapshot must not be null");
        ParserChangeAnalyzer analyzer = new ParserChangeAnalyzer();
        ParserChangeRegion region = analyzer.analyze(change, previous, previousRoot);
        if (region.fallbackRequired()) {
            AstNode fresh = fallback.reparse(snapshot);
            return new Result(fresh, true, region.fallbackReason());
        }
        // Only use incremental for simple statement-level edits inside methods
        if (!isSimpleMethodBodyEdit(region)) {
            AstNode fresh = fallback.reparse(snapshot);
            return new Result(fresh, true, "not a simple method body edit");
        }
        try {
            AstNode result = splice(previousRoot, snapshot, change, region);
            AstNodes.linkParents(result);
            return new Result(result, false, null);
        } catch (RuntimeException e) {
            AstNode fresh = fallback.reparse(snapshot);
            return new Result(fresh, true,
                    "incremental splice failed: " + e.getClass().getSimpleName() + ": " + e.getMessage());
        }
    }

    /**
     * Returns true when the change is a simple edit inside a method/constructor body
     * (local variable declaration, expression statement, etc.) where incremental
     * reparsing is safe and predictable.
     */
    private static boolean isSimpleMethodBodyEdit(ParserChangeRegion region) {
        AstNode reparsable = region.affectedNode();
        // Walk up to find the containing method/constructor
        AstNode current = reparsable;
        while (current != null) {
            if (current.kind() == AstNodeKind.METHOD_DECLARATION
                    || current.kind() == AstNodeKind.CONSTRUCTOR_DECLARATION) {
                return true;
            }
            current = current.parent();
        }
        return false;
    }

    private static String getParentChain(AstNode node) {
        StringBuilder sb = new StringBuilder();
        AstNode current = node;
        while (current != null) {
            sb.append(current.kind()).append(" -> ");
            current = current.parent();
        }
        sb.append("null");
        return sb.toString();
    }

    private AstNode splice(AstNode previousRoot,
                           DocumentSnapshot snapshot,
                           TextChange change,
                           ParserChangeRegion region) {
        AstNode previousAffected = findSameNodeInPrevious(previousRoot, region.affectedNode());
        if (previousAffected == null) {
            throw new IllegalStateException("affected node not found in previous AST");
        }
        AstNode previousReparsable = ParserChangeAnalyzer.findReparsableAncestor(previousAffected);
        if (previousReparsable == null) {
            throw new IllegalStateException("no reparsable ancestor in previous AST");
        }

        AstNode reparsedStatement = reparseRegion(snapshot, change, region);
        boolean isStatementContainer = isStatementContainer(previousReparsable.kind());
        List<AstNode> newChildren = new ArrayList<>(previousReparsable.children().size());
        boolean inserted = false;
        for (AstNode prevChild : previousReparsable.children()) {
            TextRange prevInNew = ParserChangeAnalyzer.shiftRangeToNew(prevChild.range(), change);
            if (overlapsRange(reparsedStatement.range(), prevInNew)) {
                newChildren.add(reparsedStatement);
                inserted = true;
            } else {
                newChildren.add(cloneWithShift(prevChild, change));
            }
        }
        if (!inserted) {
            newChildren.add(reparsedStatement);
        }
        newChildren.sort((a, b) -> Integer.compare(a.range().startOffset(), b.range().startOffset()));
        AstNode newReparsable = AstNode.of(previousReparsable.kind(),
                shiftRangeToNew(previousReparsable.range(), change),
                newChildren);
        return cloneWithReplacement(previousRoot, previousReparsable, newReparsable, change);
    }

    /**
     * Checks if a node kind acts as a container for statement-like children
     * (i.e., its direct children are statements/blocks that can be individually
     * spliced).
     */
    private static boolean isStatementContainer(AstNodeKind kind) {
        return kind == AstNodeKind.BLOCK
                || kind == AstNodeKind.SWITCH_CASE
                || kind == AstNodeKind.SWITCH_STATEMENT
                || kind == AstNodeKind.METHOD_DECLARATION
                || kind == AstNodeKind.CONSTRUCTOR_DECLARATION
                || kind == AstNodeKind.CLASS_DECLARATION
                || kind == AstNodeKind.INTERFACE_DECLARATION
                || kind == AstNodeKind.ENUM_DECLARATION
                || kind == AstNodeKind.RECORD_DECLARATION
                || kind == AstNodeKind.COMPILATION_UNIT;
    }

    /**
     * Checks if two ranges overlap.
     */
    private static boolean overlapsRange(TextRange a, TextRange b) {
        return a.startOffset() < b.endOffset() && b.startOffset() < a.endOffset();
    }

    /**
     * Clones {@code previousRoot}, replacing every occurrence of
     * {@code target} with {@code replacement} along the path. Ranges on
     * untouched ancestors are shifted by {@code change.delta()} when they
     * sit fully after the change.
     */
    private AstNode cloneWithReplacement(AstNode root, AstNode target, AstNode replacement,
                                          TextChange change) {
        if (root == target) return replacement;
        List<AstNode> newChildren = new ArrayList<>(root.children().size());
        for (AstNode child : root.children()) {
            if (child == target) {
                newChildren.add(replacement);
            } else {
                newChildren.add(cloneWithReplacement(child, target, replacement, change));
            }
        }
        // Always create new node - never reuse original to avoid parent link issues
        return AstNode.of(root.kind(), shiftRangeToNew(root.range(), change), newChildren, root.token());
    }

    private AstNode cloneWithShift(AstNode node, TextChange change) {
        List<AstNode> children = new ArrayList<>(node.children().size());
        for (AstNode child : node.children()) {
            children.add(cloneWithShift(child, change));
        }
        return AstNode.of(node.kind(), shiftRangeToNew(node.range(), change), children, node.token());
    }

    private boolean overlaps(TextRange range, AstNode node) {
        if (range == null) return false;
        return range.intersects(node.range());
    }

    private AstNode findSameNodeInPrevious(AstNode previousRoot, AstNode target) {
        if (target == null) return null;
        int start = target.range().startOffset();
        int end = target.range().endOffset();
        return ParserChangeAnalyzer.findDeepestContaining(previousRoot,
                TextRange.of(start, end));
    }

private AstNode reparseRegion(DocumentSnapshot snapshot,
                                  TextChange change,
                                  ParserChangeRegion region) {
        String text = snapshot.getText();
        TextRange safe = region.reparsableRange();
        int start = Math.max(0, Math.min(safe.startOffset(), text.length()));
        int end = Math.max(start, Math.min(safe.endOffset(), text.length()));
        String regionText = text.substring(start, end);

        // If the reparsable node is a BLOCK, extract only its content (without braces)
        // to avoid parsing issues with the synthetic wrapper.
        String statementText = regionText;
        if (region.affectedNode() != null) {
            AstNodeKind kind = region.affectedNode().kind();
            if (kind == AstNodeKind.BLOCK) {
                // BLOCK range includes { ... }, strip the outer braces
                if (regionText.startsWith("{") && regionText.endsWith("}")) {
                    statementText = regionText.substring(1, regionText.length() - 1);
                }
            }
        }

        String synthetic = "class __Synthetic__ { void m() { " + statementText + " } }";
        DocumentSnapshot fakeDoc = DocumentSnapshot.oneShot(synthetic);
        var lex = lexer.lex(fakeDoc);
        JavaTokenStream stream = new JavaTokenStream(lex.tokens(), synthetic);
        JavaParser parser = new JavaParser(stream);
        AstNode root = parser.parse().getAstRoot();
        AstNode clazz = findChild(root, AstNodeKind.CLASS_DECLARATION);
        AstNode method = findChild(clazz, AstNodeKind.METHOD_DECLARATION);
        AstNode block = findChild(method, AstNodeKind.BLOCK);
        if (block == null || block.children().isEmpty()) {
            throw new IllegalStateException("could not isolate reparsable region");
        }
        if (block.children().size() > 1) {
            throw new IllegalStateException(
                    "reparsable region contains multiple statements — refusing to guess");
        }
        AstNode statement = block.children().get(0);
        TextRange stmtRange = statement.range();
        int stmtStartInFake = stmtRange.startOffset();
        int preamble = "class __Synthetic__ { void m() { ".length();
        int originalStart = stmtStartInFake - preamble;
        int originalEnd = stmtRange.endOffset() - preamble;

        // Build a fresh copy with ranges shifted from synthetic coords to NEW-text coords
        List<AstNode> freshChildren = new ArrayList<>(statement.children().size());
        for (AstNode child : statement.children()) {
            freshChildren.add(cloneWithoutParents(child, stmtStartInFake, start));
        }
        AstNode freshStatement = AstNode.of(statement.kind(),
                TextRange.of(originalStart + start, originalEnd + start),
                freshChildren,
                statement.token());

        return freshStatement;
    }

    /**
     * Creates a deep copy of a node and its children with ranges shifted
     * from synthetic coordinates to NEW-text coordinates.
     * Synthetic coordinate = node.range().
     * NEW-text coordinate = (node.start - stmtStartInFake) + start.
     */
    private AstNode cloneWithoutParents(AstNode node, int stmtStartInFake, int start) {
        List<AstNode> children = new ArrayList<>(node.children().size());
        for (AstNode child : node.children()) {
            children.add(cloneWithoutParents(child, stmtStartInFake, start));
        }
        int relativeStart = node.range().startOffset() - stmtStartInFake;
        int relativeEnd = node.range().endOffset() - stmtStartInFake;
        int newStart = relativeStart + start;
        int newEnd = relativeEnd + start;
        return AstNode.of(node.kind(),
                TextRange.of(newStart, newEnd),
                children,
                node.token());
    }

    /**
     * Shifts children from synthetic-relative coordinates to NEW-text
     * coordinates. Each child's position is offset relative to the
     * statement's start in the synthetic (where the synthetic preamble
     * has already been subtracted), then {@code start} is added to
     * anchor the result in the NEW text.
     */
    private List<AstNode> shiftChildren(List<AstNode> children, int stmtStartInFake, int start) {
        List<AstNode> result = new ArrayList<>(children.size());
        for (AstNode child : children) {
            TextRange r = child.range();
            int relativeStart = r.startOffset() - stmtStartInFake;
            int relativeEnd = r.endOffset() - stmtStartInFake;
            result.add(AstNode.of(child.kind(),
                    TextRange.of(relativeStart + start, relativeEnd + start),
                    shiftChildren(child.children(), stmtStartInFake, start),
                    child.token()));
        }
        return result;
    }

    private List<AstNode> shiftChildren(List<AstNode> children, int delta) {
        List<AstNode> result = new ArrayList<>(children.size());
        for (AstNode child : children) {
            TextRange r = child.range();
            result.add(AstNode.of(child.kind(),
                    TextRange.of(r.startOffset() + delta, r.endOffset() + delta),
                    shiftChildren(child.children(), delta),
                    child.token()));
        }
        return result;
    }

    private AstNode findChild(AstNode parent, AstNodeKind kind) {
        if (parent == null) return null;
        for (AstNode child : parent.children()) {
            if (child.kind() == kind) return child;
        }
        return null;
    }

    private static TextRange shiftRangeToNew(TextRange range, TextChange change) {
        return ParserChangeAnalyzer.shiftRangeToNew(range, change);
    }

    /**
     * Outcome of a {@link #parse} call: the rebuilt AST root, whether the
     * strategy fell back to a full reparse, and (in the fallback case) a
     * short reason useful for diagnostics and tests.
     */
    public record Result(AstNode astRoot, boolean fallbackUsed, String fallbackReason) {
    }
}
