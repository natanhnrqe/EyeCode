package com.eyecode.language.java.parser.incremental;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.ast.AstNode;
import com.eyecode.language.ast.AstNodeKind;

import java.util.Objects;

/**
 * Triages a {@link TextChange} and decides where in the AST the change
 * lives, plus how much of the tree can be safely rebuilt incrementally.
 * <p>
 * The analyzer is intentionally conservative: every "easy" rule is biased
 * toward full reparse so the incremental path only runs when the change
 * is unambiguous. The triage rules (in order):
 * <ol>
 *   <li>missing inputs → full reparse;</li>
 *   <li>empty change → reparse the smallest enclosing reparsable region
 *       (usually the surrounding statement), or the whole compilation
 *       unit when nothing safer is available;</li>
 *   <li>change straddles a structural boundary (insertion/removal of
 *       {@code {}/()};{@code /}{ @code }/;{@code }/</code>}) → full
 *       reparse, because incremental reparsing cannot reliably keep the
 *       delimiter tree in sync;</li>
 *   <li>change crosses multiple methods or types → full reparse;</li>
 *   <li>deepest AST node containing the change lives outside any reparsable
 *       region (e.g. annotation, modifier, parameter type) → full reparse;</li>
 *   <li>otherwise → incremental reparse of the enclosing block's statement
 *       region.</li>
 * </ol>
 * The reparsable region is always either a single BLOCK / METHOD /
 * CLASS_DECLARATION / COMPILATION_UNIT node, narrowed to the smallest
 * reparsable statement when possible.
 */
public final class ParserChangeAnalyzer {

    public ParserChangeRegion analyze(TextChange change,
                                      DocumentSnapshot previous,
                                      AstNode previousRoot) {
        Objects.requireNonNull(change, "change must not be null");
        if (previousRoot == null || previous == null) {
            return ParserChangeRegion.fullReparse(
                    change.removedRange(), change.resultingRange(), null,
                    "no previous AST available");
        }
        if (change.isEmpty()) {
            return ParserChangeRegion.fullReparse(
                    change.removedRange(), change.resultingRange(), null,
                    "empty change — full reparse is the safe default");
        }

        TextRange oldRange = change.removedRange();
        TextRange newRange = change.resultingRange();
        String oldText = previous.getText();
        String actualRemoved = safeSubstring(oldText, oldRange.startOffset(), oldRange.endOffset());
        String actualInserted = change.insertedText() == null ? "" : change.insertedText();

        if (containsStructuralDelimiter(actualRemoved, actualInserted)) {
            return ParserChangeRegion.fullReparse(
                    oldRange, newRange, null,
                    "change touches a structural delimiter");
        }

        AstNode deepest = findDeepestContaining(previousRoot, oldRange);
        if (deepest == null) {
            return ParserChangeRegion.fullReparse(
                    oldRange, newRange, null,
                    "change is outside the previous AST");
        }

        AstNode reparsable = findReparsableAncestor(deepest);
        if (reparsable == null) {
            return ParserChangeRegion.fullReparse(
                    oldRange, newRange, deepest,
                    "no structural ancestor is safe to incrementally reparse");
        }

        TextRange safeRange = clampReparsableRange(reparsable, newRange, change);
        return ParserChangeRegion.incremental(oldRange, newRange, deepest, safeRange);
    }

    private static String safeSubstring(String text, int start, int end) {
        int s = Math.max(0, Math.min(start, text.length()));
        int e = Math.max(s, Math.min(end, text.length()));
        return text.substring(s, e);
    }

    /**
     * True when the text touches a structural delimiter. Conservative:
     * matches braces, parens, brackets, and semicolons (any of which may
     * change the statement/block structure when added or removed). The
     * cost of being wrong here is one full reparse — the cost of being
     * too lax is an inconsistent AST.
     */
    private static boolean containsStructuralDelimiter(String removed, String inserted) {
        if (removed != null && !removed.isEmpty()) {
            for (int i = 0; i < removed.length(); i++) {
                char c = removed.charAt(i);
                if (c == '{' || c == '}' || c == '(' || c == ')'
                        || c == '[' || c == ']' || c == ';') {
                    return true;
                }
            }
        }
        if (inserted != null && !inserted.isEmpty()) {
            for (int i = 0; i < inserted.length(); i++) {
                char c = inserted.charAt(i);
                if (c == '{' || c == '}' || c == '(' || c == ')'
                        || c == '[' || c == ']' || c == ';') {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Walks the AST and returns the deepest node whose {@link AstNode#range()}
     * strictly contains the change range. Returns {@code null} when the
     * range is outside the root.
     */
    public static AstNode findDeepestContaining(AstNode root, TextRange range) {
        if (root == null || range == null) return null;
        if (!root.range().contains(range.startOffset())
                || !root.range().contains(range.endOffset())) {
            return null;
        }
        AstNode current = root;
        while (true) {
            AstNode next = null;
            for (AstNode child : current.children()) {
                TextRange childRange = child.range();
                if (childRange.contains(range.startOffset())
                        && childRange.contains(range.endOffset())) {
                    next = child;
                    break;
                }
            }
            if (next == null) return current;
            current = next;
        }
    }

    /**
     * Walks up the tree from {@code node} until it finds an ancestor that
     * is structurally safe to incrementally reparse. The walk stops at
     * the first BLOCK / METHOD_DECLARATION / CONSTRUCTOR_DECLARATION /
     * CLASS_DECLARATION (or any *_DECLARATION in the type family) /
     * COMPILATION_UNIT. When the starting node IS one of those kinds, the
     * node itself is returned.
     */
    public static AstNode findReparsableAncestor(AstNode node) {
        AstNode current = node;
        while (current != null) {
            if (isReparsable(current)) return current;
            current = current.parent();
        }
        return null;
    }

    private static boolean isReparsable(AstNode node) {
        AstNodeKind kind = node.kind();
        return kind == AstNodeKind.COMPILATION_UNIT
                || kind == AstNodeKind.CLASS_DECLARATION
                || kind == AstNodeKind.INTERFACE_DECLARATION
                || kind == AstNodeKind.ENUM_DECLARATION
                || kind == AstNodeKind.RECORD_DECLARATION
                || kind == AstNodeKind.METHOD_DECLARATION
                || kind == AstNodeKind.CONSTRUCTOR_DECLARATION
                || kind == AstNodeKind.BLOCK
                || kind == AstNodeKind.SWITCH_STATEMENT
                || kind == AstNodeKind.SWITCH_CASE;
    }

    /**
     * Narrows the reparsable range to the smallest statement inside the
     * reparsable node that contains the change, when one exists. The
     * resulting range is in the NEW text's coordinate space.
     */
    private static TextRange clampReparsableRange(AstNode reparsable,
                                                  TextRange newRange,
                                                  TextChange change) {
        AstNode containing = findStatementInside(reparsable, newRange);
        if (containing != null) {
            return shiftRangeToNew(containing.range(), change);
        }
        return shiftRangeToNew(reparsable.range(), change);
    }

    private static AstNode findStatementInside(AstNode parent, TextRange range) {
        AstNode result = null;
        for (AstNode child : parent.children()) {
            TextRange cr = child.range();
            if (cr.contains(range.startOffset()) && cr.contains(range.endOffset())) {
                if (isStatementLike(child)) {
                    result = child;
                    break;
                }
                AstNode deeper = findStatementInside(child, range);
                if (deeper != null) {
                    result = deeper;
                }
            }
        }
        if (result == null && isStatementLike(parent)) {
            return parent;
        }
        return result;
    }

    private static boolean isStatementLike(AstNode node) {
        AstNodeKind kind = node.kind();
        return kind == AstNodeKind.BLOCK
                || kind == AstNodeKind.LOCAL_VARIABLE_DECLARATION
                || kind == AstNodeKind.FIELD_DECLARATION
                || kind == AstNodeKind.EXPRESSION_STATEMENT
                || kind == AstNodeKind.IF_STATEMENT
                || kind == AstNodeKind.WHILE_STATEMENT
                || kind == AstNodeKind.DO_WHILE_STATEMENT
                || kind == AstNodeKind.FOR_STATEMENT
                || kind == AstNodeKind.ENHANCED_FOR_STATEMENT
                || kind == AstNodeKind.RETURN_STATEMENT
                || kind == AstNodeKind.BREAK_STATEMENT
                || kind == AstNodeKind.CONTINUE_STATEMENT
                || kind == AstNodeKind.THROW_STATEMENT
                || kind == AstNodeKind.TRY_STATEMENT
                || kind == AstNodeKind.SWITCH_STATEMENT
                || kind == AstNodeKind.SYNCHRONIZED_STATEMENT
                || kind == AstNodeKind.LABELED_STATEMENT
                || kind == AstNodeKind.YIELD_STATEMENT
                || kind == AstNodeKind.ASSERT_STATEMENT
                || kind == AstNodeKind.METHOD_DECLARATION
                || kind == AstNodeKind.CONSTRUCTOR_DECLARATION
                || kind == AstNodeKind.CLASS_DECLARATION
                || kind == AstNodeKind.COMPILATION_UNIT;
    }

    /**
     * Shifts a range from OLD coordinates to NEW coordinates using the
     * change's {@code delta()}. Ranges fully before the change keep
     * their offsets (no shift). Ranges fully after the change shift by
     * the delta. Ranges that overlap the change have their start
     * clamped to the change's start and end shifted by the delta.
     */
    static TextRange shiftRangeToNew(TextRange range, TextChange change) {
        int delta = change.delta();
        int start = range.startOffset();
        int end = range.endOffset();
        int changeStart = change.removedRange().startOffset();
        int changeEnd = change.removedRange().endOffset();
        if (end <= changeStart) {
            return range;
        }
        if (start >= changeEnd) {
            return TextRange.of(start + delta, end + delta);
        }
        int newStart = Math.min(start, changeStart);
        int newEnd = end + delta;
        return TextRange.of(newStart, newEnd);
    }
}
