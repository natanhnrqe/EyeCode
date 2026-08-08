package com.eyecode.editor.intelligence.selection;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.editor.intelligence.document.TextRange;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Java-first {@link SelectionExpander} built exclusively on lexical heuristics.
 * <p>
 * Level ladder (all levels are computed from the snapshot alone, never by
 * mutating anything):
 * <ol>
 *   <li>{@code word} — identifier boundaries around the caret;</li>
 *   <li>{@code expression} — dot chains ({@code bar → foo.bar()}), call suffixes
 *       and single-character binary operators ({@code a → a + b});</li>
 *   <li>{@code arguments} — full delimited content of the innermost enclosing
 *       {@code ()}/{@code []}/initializer-{@code {}} pair ({@code a + b →
 *       a + b, c});</li>
 *   <li>{@code delimiters} — the enclosing pair itself ({@code (a + b, c)});</li>
 *   <li>{@code statement} — the logical statement up to its terminating
 *       {@code ;} ({@code return calculate(a + b, c);});</li>
 *   <li>{@code block} — the enclosing {@code { ... }};</li>
 *   <li>{@code declaration} — the enclosing method/class declaration, or the
 *       whole document when no block exists.</li>
 * </ol>
 * Strings, character literals and comments are masked out by
 * {@link SelectionLexicon} (the same lexical states as {@code IndentContext}),
 * so delimiters inside literals are never treated as structure.
 * <p>
 * Known heuristics limits (no parser, by design): control-flow blocks
 * ({@code if}/{@code for}/...) jump straight to the enclosing declaration at
 * level 7; generic angle brackets and assignment operators are not part of the
 * level-2 operator set; for-loop headers extend through the parens at level 5.
 */
public final class JavaSelectionExpander implements SelectionExpander {

    public static final int MAX_LEVEL = 7;

    private static final Set<Character> OPERATORS = Set.of(
            '+', '-', '*', '/', '%', '&', '|', '^', '!', '~', '?', ':'
    );

    private static final Set<String> DECLARATION_KEYWORDS = Set.of(
            "public", "private", "protected", "static", "final", "abstract",
            "synchronized", "native", "transient", "volatile", "strictfp",
            "sealed", "non-sealed", "default",
            "class", "interface", "enum", "record", "void",
            "byte", "short", "int", "long", "float", "double",
            "char", "boolean", "var"
    );

    private record Pair(int open, int close) {
    }

    @Override
    public int maxLevel() {
        return MAX_LEVEL;
    }

    @Override
    public Optional<TextRange> expand(DocumentSnapshot snapshot,
                                      int caretOffset,
                                      Optional<TextRange> selection,
                                      int level) {
        if (snapshot == null || level < 1 || level > MAX_LEVEL) {
            return Optional.empty();
        }
        String text = snapshot.getText();
        int length = text.length();
        int caret = Math.max(0, Math.min(caretOffset, length));
        Optional<TextRange> base = clamp(snapshot, selection);
        boolean[] code = SelectionLexicon.codeMask(text);
        return switch (level) {
            case 1 -> wordAt(text, code, caret);
            case 2 -> expression(text, code, base, caret);
            case 3 -> argsContent(text, code, base);
            case 4 -> delimiterWrap(text, code, base);
            case 5 -> statement(text, code, snapshot.lineMap(), base, caret);
            case 6 -> enclosingBlock(text, code, base, caret);
            case 7 -> structuralDeclaration(text, code, snapshot.lineMap(), base, caret);
            default -> Optional.empty();
        };
    }

    private static Optional<TextRange> clamp(DocumentSnapshot snapshot, Optional<TextRange> selection) {
        if (selection == null || selection.isEmpty()) {
            return Optional.empty();
        }
        int length = snapshot.length();
        TextRange range = selection.get();
        return Optional.of(new TextRange(
                Math.max(0, Math.min(range.startOffset(), length)),
                Math.max(0, Math.min(range.endOffset(), length))
        ));
    }

    private static Optional<TextRange> wordAt(String text, boolean[] code, int offset) {
        int length = text.length();
        if (offset < 0 || offset > length) {
            return Optional.empty();
        }
        int start = offset;
        while (start > 0 && code[start - 1] && isWordChar(text.charAt(start - 1))) {
            start--;
        }
        int end = offset;
        while (end < length && code[end] && isWordChar(text.charAt(end))) {
            end++;
        }
        if (start == end) {
            return Optional.empty();
        }
        return Optional.of(new TextRange(start, end));
    }

    private static Optional<TextRange> expression(String text,
                                                  boolean[] code,
                                                  Optional<TextRange> base,
                                                  int caret) {
        TextRange range = base.orElse(wordAt(text, code, caret).orElse(null));
        if (range == null || range.isEmpty()) {
            return Optional.empty();
        }
        int start = expressionStart(text, code, range.startOffset());
        int end = expressionEnd(text, code, range.endOffset());
        return Optional.of(new TextRange(start, end));
    }

    private static Optional<TextRange> argsContent(String text, boolean[] code, Optional<TextRange> base) {
        if (base.isEmpty()) {
            return Optional.empty();
        }
        TextRange range = base.get();
        for (Pair pair : enclosingPairs(text, code, range.startOffset())) {
            boolean contentEquals = pair.open + 1 == range.startOffset() && pair.close == range.endOffset();
            boolean contentContains = pair.open + 1 <= range.startOffset() && pair.close >= range.endOffset();
            if (contentContains && !contentEquals) {
                return Optional.of(new TextRange(pair.open + 1, pair.close));
            }
        }
        return Optional.empty();
    }

    private static Optional<TextRange> delimiterWrap(String text, boolean[] code, Optional<TextRange> base) {
        if (base.isEmpty()) {
            return Optional.empty();
        }
        TextRange range = base.get();
        for (Pair pair : enclosingPairs(text, code, range.startOffset())) {
            if (pair.open + 1 <= range.startOffset() && pair.close >= range.endOffset()) {
                return Optional.of(new TextRange(pair.open, pair.close + 1));
            }
        }
        return Optional.empty();
    }

    private static Optional<TextRange> statement(String text,
                                                 boolean[] code,
                                                 LineMap map,
                                                 Optional<TextRange> base,
                                                 int caret) {
        boolean hasBase = base.isPresent() && !base.get().isEmpty();
        int baseStart = hasBase ? base.get().startOffset() : caret;
        int baseEnd = hasBase ? base.get().endOffset() : caret;
        int start = statementStart(text, code, baseStart);
        int end = baseEnd;
        List<Pair> pairs = enclosingPairs(text, code, baseStart);
        for (int i = pairs.size() - 1; i >= 0; i--) {
            Pair pair = pairs.get(i);
            if (pair.open < baseStart && pair.close >= baseEnd) {
                end = pair.close + 1;
                break;
            }
        }
        end = statementEnd(text, code, end);
        return Optional.of(new TextRange(Math.min(start, end), Math.max(start, end)));
    }

    private static Optional<TextRange> enclosingBlock(String text,
                                                      boolean[] code,
                                                      Optional<TextRange> base,
                                                      int caret) {
        boolean hasBase = base.isPresent() && !base.get().isEmpty();
        int from = hasBase ? base.get().startOffset() : caret;
        int depth = 0;
        for (int i = from - 1; i >= 0; i--) {
            if (!code[i]) {
                continue;
            }
            char c = text.charAt(i);
            if (c == '}') {
                depth++;
            } else if (c == '{') {
                if (depth == 0) {
                    if (isInitializerBrace(text, code, i)) {
                        continue;
                    }
                    int close = matchClose(text, code, i, '{');
                    if (close >= 0) {
                        return Optional.of(new TextRange(i, close + 1));
                    }
                } else {
                    depth--;
                }
            }
        }
        return Optional.empty();
    }

    private static Optional<TextRange> structuralDeclaration(String text,
                                                             boolean[] code,
                                                             LineMap map,
                                                             Optional<TextRange> base,
                                                             int caret) {
        Optional<TextRange> block = enclosingBlock(text, code, base, caret);
        if (block.isEmpty()) {
            return Optional.of(new TextRange(0, text.length()));
        }
        int length = text.length();
        int openLine = map.lineOfOffset(block.get().startOffset());
        int declarationStart = -1;
        for (int line = openLine; line >= 0; line--) {
            String trimmed = text.substring(map.lineStartOffset(line), map.lineEndOffset(line)).strip();
            if (trimmed.isEmpty() || trimmed.startsWith("}") || trimmed.startsWith(";")) {
                break;
            }
            if (trimmed.startsWith("@") || startsWithDeclarationKeyword(trimmed)) {
                declarationStart = map.lineStartOffset(line);
                break;
            }
        }
        if (declarationStart < 0) {
            return Optional.of(new TextRange(0, length));
        }
        int bodyOpen = -1;
        int paren = 0;
        int bracket = 0;
        for (int i = declarationStart; i < length; i++) {
            if (!code[i]) {
                continue;
            }
            char c = text.charAt(i);
            if (c == '(') {
                paren++;
            } else if (c == ')') {
                paren = Math.max(0, paren - 1);
            } else if (c == '[') {
                bracket++;
            } else if (c == ']') {
                bracket = Math.max(0, bracket - 1);
            } else if (c == '{' && paren == 0 && bracket == 0) {
                bodyOpen = i;
                break;
            }
        }
        if (bodyOpen < 0) {
            return Optional.of(new TextRange(declarationStart, block.get().endOffset()));
        }
        int close = matchClose(text, code, bodyOpen, '{');
        if (close < 0) {
            return Optional.of(new TextRange(declarationStart, block.get().endOffset()));
        }
        return Optional.of(new TextRange(declarationStart, close + 1));
    }

    private static int expressionStart(String text, boolean[] code, int from) {
        int i = from;
        while (i > 0) {
            int j = i - 1;
            while (j >= 0 && (!code[j] || Character.isWhitespace(text.charAt(j)))) {
                j--;
            }
            if (j < 0) {
                return 0;
            }
            char c = text.charAt(j);
            if (c == ')') {
                int open = matchOpen(text, code, j, ')');
                if (open < 0) {
                    return i;
                }
                i = open;
            } else if (c == ']') {
                int open = matchOpen(text, code, j, ']');
                if (open < 0) {
                    return i;
                }
                i = open;
            } else if (isWordChar(c)) {
                i = j;
                while (i > 0 && code[i - 1] && isWordChar(text.charAt(i - 1))) {
                    i--;
                }
            } else if (c == '.') {
                i = j;
            } else if (isOpChar(c)) {
                int k = j;
                while (k >= 0 && code[k] && isOpChar(text.charAt(k))) {
                    k--;
                }
                int m = k;
                while (m >= 0 && (!code[m] || Character.isWhitespace(text.charAt(m)))) {
                    m--;
                }
                if (m < 0) {
                    i = k + 1;
                    continue;
                }
                char before = text.charAt(m);
                if (before == '(' || before == '[' || before == ',' || before == ';'
                        || before == '{' || before == '}' || before == '=') {
                    i = k + 1;
                    continue;
                }
                i = m + 1;
            } else {
                return i;
            }
        }
        return 0;
    }

    private static int expressionEnd(String text, boolean[] code, int from) {
        int length = text.length();
        int i = from;
        while (i < length) {
            int j = i;
            while (j < length && (!code[j] || Character.isWhitespace(text.charAt(j)))) {
                j++;
            }
            if (j >= length) {
                return length;
            }
            char c = text.charAt(j);
            if (c == '(' || c == '[') {
                int close = matchClose(text, code, j, c);
                if (close < 0) {
                    return i;
                }
                i = close + 1;
            } else if (isWordChar(c)) {
                i = j;
                while (i < length && code[i] && isWordChar(text.charAt(i))) {
                    i++;
                }
            } else if (c == '.') {
                i = j + 1;
            } else if (isOpChar(c)) {
                int k = j;
                while (k < length && code[k] && isOpChar(text.charAt(k))) {
                    k++;
                }
                int m = k;
                while (m < length && (!code[m] || Character.isWhitespace(text.charAt(m)))) {
                    m++;
                }
                if (m >= length) {
                    return i;
                }
                char after = text.charAt(m);
                if (isWordChar(after) || after == '.' || after == '(' || after == '[') {
                    i = m;
                } else {
                    return i;
                }
            } else {
                return i;
            }
        }
        return length;
    }

    private static int statementStart(String text, boolean[] code, int from) {
        int stop = -1;
        for (int i = from - 1; i >= 0; i--) {
            if (!code[i] || Character.isWhitespace(text.charAt(i))) {
                continue;
            }
            char c = text.charAt(i);
            if (c == ';' || c == '}') {
                stop = i;
                break;
            }
            if (c == '{' && !isInitializerBrace(text, code, i)) {
                stop = i;
                break;
            }
        }
        int start = stop + 1;
        while (start < text.length() && (!code[start] || Character.isWhitespace(text.charAt(start)))) {
            start++;
        }
        return start;
    }

    private static int statementEnd(String text, boolean[] code, int from) {
        int length = text.length();
        int i = from;
        int paren = 0;
        int bracket = 0;
        int brace = 0;
        while (i < length) {
            char c = text.charAt(i);
            if (!code[i] || Character.isWhitespace(c)) {
                i++;
                continue;
            }
            if (c == '(') {
                paren++;
            } else if (c == '[') {
                bracket++;
            } else if (c == '{') {
                if (isInitializerBrace(text, code, i)) {
                    brace++;
                } else {
                    return i;
                }
            } else if (c == ')') {
                paren = Math.max(0, paren - 1);
            } else if (c == ']') {
                bracket = Math.max(0, bracket - 1);
            } else if (c == '}') {
                brace = Math.max(0, brace - 1);
            } else if (c == ';') {
                if (paren == 0 && bracket == 0 && brace == 0) {
                    return i + 1;
                }
            } else if (c == ',') {
                if (paren == 0 && bracket == 0 && brace == 0) {
                    return i;
                }
            }
            i++;
        }
        return length;
    }

    private static List<Pair> enclosingPairs(String text, boolean[] code, int from) {
        List<Pair> pairs = new ArrayList<>();
        int depth = 0;
        for (int i = from - 1; i >= 0; i--) {
            if (!code[i]) {
                continue;
            }
            char c = text.charAt(i);
            if (c == ')' || c == ']' || c == '}') {
                depth++;
            } else if (c == '(' || c == '[' || c == '{') {
                if (depth == 0) {
                    if (c == '{' && !isInitializerBrace(text, code, i)) {
                        continue;
                    }
                    int close = matchClose(text, code, i, c);
                    if (close >= 0) {
                        pairs.add(new Pair(i, close));
                    }
                } else {
                    depth--;
                }
            }
        }
        return pairs;
    }

    private static boolean isInitializerBrace(String text, boolean[] code, int open) {
        int i = open - 1;
        while (i >= 0 && (!code[i] || Character.isWhitespace(text.charAt(i)))) {
            i--;
        }
        if (i < 0) {
            return false;
        }
        char c = text.charAt(i);
        return c == '=' || c == ',' || c == '(' || c == '[' || c == ':';
    }

    private static int matchOpen(String text, boolean[] code, int close, char closeChar) {
        char openChar = closeChar == ')' ? '(' : closeChar == ']' ? '[' : '{';
        int depth = 0;
        for (int i = close - 1; i >= 0; i--) {
            if (!code[i]) {
                continue;
            }
            char c = text.charAt(i);
            if (c == closeChar) {
                depth++;
            } else if (c == openChar) {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return -1;
    }

    private static int matchClose(String text, boolean[] code, int open, char openChar) {
        char closeChar = openChar == '(' ? ')' : openChar == '[' ? ']' : '}';
        int depth = 0;
        for (int i = open + 1; i < text.length(); i++) {
            if (!code[i]) {
                continue;
            }
            char c = text.charAt(i);
            if (c == openChar) {
                depth++;
            } else if (c == closeChar) {
                if (depth == 0) {
                    return i;
                }
                depth--;
            }
        }
        return -1;
    }

    private static boolean isWordChar(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    private static boolean isOpChar(char c) {
        return OPERATORS.contains(c);
    }

    private static boolean startsWithDeclarationKeyword(String trimmed) {
        for (String keyword : DECLARATION_KEYWORDS) {
            if (trimmed.startsWith(keyword)) {
                int after = keyword.length();
                if (after >= trimmed.length() || isKeywordBoundary(trimmed.charAt(after))) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean isKeywordBoundary(char c) {
        return c == '(' || c == '{' || c == '<' || Character.isWhitespace(c);
    }
}
