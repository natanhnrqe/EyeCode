package com.eyecode.editor.v2.completion;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * Ranks completion items using a subsequence-based fuzzy scoring algorithm.
 *
 * <p>Scoring is computed by finding the best character-alignment of the query
 * inside a candidate string (label or detail), giving bonuses for:
 * <ul>
 *   <li>Word boundary starts (after space, dot, underscore, '(' etc.)</li>
 *   <li>CamelCase boundary starts (uppercase preceded by lowercase)</li>
 *   <li>Contiguous character runs</li>
 *   <li>Exact-case matches</li>
 * </ul>
 * and penalising large gaps. The final score is further boosted by the item's
 * declared priority and kind/category weight.
 */
public final class CompletionRanking {

    // ── Scoring constants ─────────────────────────────────────────────────────
    private static final int BASE_CHAR        = 10;
    private static final int BOUNDARY_BONUS   = 80;
    private static final int CAMEL_BONUS      = 60;
    private static final int CONTIGUOUS_BONUS = 40;
    private static final int EXACT_CASE_BONUS = 10;
    private static final int GAP_PENALTY      = 5;
    private static final int AUTO_MIN_SCORE   = 90;

    // ── Public API ────────────────────────────────────────────────────────────

    public List<CompletionItem> rank(List<CompletionItem> items, String query) {
        return rank(items, query, false);
    }

    public List<CompletionItem> rank(List<CompletionItem> items, String query, boolean manual) {
        String q = query == null ? "" : query;
        boolean emptyQuery = q.isEmpty();

        // Auto-completion with no prefix → nothing to show
        if (emptyQuery && !manual) {
            return List.of();
        }

        return items.stream()
                .map(item -> {
                    if (emptyQuery) {
                        return new ScoredItem(item, emptyScore(item), 0);
                    }
                    // labelScore is always computed; detailScore only for manual invocation.
                    int labelScore  = fuzzyScore(q, item.getLabel());
                    int detailScore = manual && item.getDetail() != null
                            ? fuzzyScore(q, item.getDetail()) : 0;
                    int matchScore = Math.max(labelScore, detailScore);
                    int total = matchScore > 0 ? matchScore + itemBoost(item) : 0;
                    return new ScoredItem(item, total, labelScore);
                })
                // For auto: filter by raw labelScore so boosts can't compensate weak matches.
                // For manual: any positive matchScore (stored as total > 0) is accepted.
                .filter(s -> {
                    if (emptyQuery) return true;
                    if (manual) return s.score > 0;
                    return s.rawLabelScore >= AUTO_MIN_SCORE;
                })
                .sorted(Comparator.comparingInt((ScoredItem s) -> -s.score))
                .map(s -> s.item)
                .toList();
    }

    // ── Scoring ───────────────────────────────────────────────────────────────

    /** Score used when prefix is empty: kind + priority weight only. */
    private int emptyScore(CompletionItem item) {
        return itemBoost(item);
    }

    /** Item-level boost: kind weight + declared priority. */
    private int itemBoost(CompletionItem item) {
        int kindBoost = switch (item.getKind()) {
            case VARIABLE             -> 30;
            case FIELD                -> 25;
            case METHOD, CONSTRUCTOR  -> 20;
            case CLASS, INTERFACE, ENUM, RECORD -> 15;
            case KEYWORD              -> 10;
            case SNIPPET              -> 5;
            default                   -> 0;
        };
        return kindBoost + item.getPriority();
    }

    /**
     * Returns the best alignment score for {@code query} as a subsequence of
     * {@code target}, or {@code 0} if the query is not a subsequence at all.
     *
     * <p>The algorithm uses a greedy first-pass to verify membership and then
     * a recursive best-path search (with memoisation) to find the optimal
     * character alignment score.
     */
    private int fuzzyScore(String query, String target) {
        if (query.isEmpty()) return 0;
        if (target == null || target.isEmpty()) return 0;

        String qLow = query.toLowerCase();
        String tLow = target.toLowerCase();

        // Quick subsequence check before the expensive recursive pass
        if (!isSubsequence(qLow, tLow)) return 0;

        // Compute best-alignment score
        int[][] memo = new int[qLow.length()][tLow.length()];
        for (int[] row : memo) Arrays.fill(row, Integer.MIN_VALUE);
        int result = bestScore(query, target, qLow, tLow, 0, 0, -1, memo);
        return result == Integer.MIN_VALUE ? 0 : result;
    }

    /**
     * Recursive best-path alignment with memoisation.
     * {@code qi} = next query index to match, {@code ti} = current target index,
     * {@code prevTi} = target index of the last matched character (-1 if none).
     */
    private int bestScore(String query, String target,
                          String qLow, String tLow,
                          int qi, int ti, int prevTi,
                          int[][] memo) {
        if (qi == qLow.length()) return 0;                     // all query chars matched
        if (ti >= tLow.length()) return Integer.MIN_VALUE;     // ran out of target chars

        // Only memoize from start (prevTi == -1) to keep memo table simple
        if (prevTi == -1 && memo[qi][ti] != Integer.MIN_VALUE) {
            return memo[qi][ti];
        }

        int best = Integer.MIN_VALUE;

        for (int i = ti; i < tLow.length(); i++) {
            if (tLow.charAt(i) != qLow.charAt(qi)) continue;

            // Gap penalty since last match
            int gap = (prevTi < 0) ? 0 : (i - prevTi - 1);
            int charScore = BASE_CHAR
                    - gap * GAP_PENALTY
                    + boundaryBonus(target, i)
                    + contiguousBonus(prevTi, i)
                    + exactCaseBonus(query.charAt(qi), target.charAt(i));

            int rest = bestScore(query, target, qLow, tLow, qi + 1, i + 1, i, memo);
            if (rest != Integer.MIN_VALUE) {
                int total = charScore + rest;
                if (total > best) best = total;
            }
        }

        if (prevTi == -1) memo[qi][ti] = best;
        return best;
    }

    // ── Bonus helpers ─────────────────────────────────────────────────────────

    private int boundaryBonus(String target, int i) {
        if (i == 0) return BOUNDARY_BONUS;
        char prev = target.charAt(i - 1);
        char curr = target.charAt(i);
        // After separator character (word boundary)
        if (prev == ' ' || prev == '.' || prev == '_' || prev == '(' || prev == '<') {
            return BOUNDARY_BONUS;
        }
        // CamelCase boundary: current is uppercase, previous is lowercase
        if (Character.isUpperCase(curr) && Character.isLowerCase(prev)) {
            return CAMEL_BONUS;
        }
        return 0;
    }

    private int contiguousBonus(int prevTi, int currentTi) {
        return (prevTi >= 0 && currentTi == prevTi + 1) ? CONTIGUOUS_BONUS : 0;
    }

    private int exactCaseBonus(char qc, char tc) {
        return (qc == tc) ? EXACT_CASE_BONUS : 0;
    }

    // ── Utility ───────────────────────────────────────────────────────────────

    /** Returns true if {@code q} is a subsequence of {@code t} (both lower-cased). */
    private boolean isSubsequence(String q, String t) {
        int qi = 0;
        for (int ti = 0; ti < t.length() && qi < q.length(); ti++) {
            if (t.charAt(ti) == q.charAt(qi)) qi++;
        }
        return qi == q.length();
    }

    private record ScoredItem(CompletionItem item, int score, int rawLabelScore) {}
}
