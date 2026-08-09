package com.eyecode.language.java.incremental;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.Token;
import com.eyecode.language.java.LexerSnapshot;

import java.util.ArrayList;
import java.util.List;

/**
 * Incremental Java lexer.
 * <p>
 * Given the previous text, its lexical snapshot and the new text, re-lexes
 * only the region that could be affected by the change and reuses the
 * untouched token regions. The result is guaranteed to be identical to a full
 * re-lex of the new text:
 * <ul>
 *   <li>the re-lex starts at a safe token boundary (the edit boundary itself,
 *       or the start of the token containing it when the edit falls inside a
 *       string/character/comment);</li>
 *   <li>tokens are compared against the previous stream after applying the
 *       edit delta; a token that matches (type, text, mapped offset) and lies
 *       entirely after the removed region proves the streams are re-aligned —
 *       beyond that point the texts are identical, so the previous tail is
 *       reused unchanged (offsets shifted by the delta);</li>
 *   <li>when no stabilization can be proven the window widens, and at full
 *       coverage the pass is equivalent to a full re-lex.</li>
 * </ul>
 * All scanning is done by the base {@link com.eyecode.language.java.JavaLexer}
 * on substrings; no tokenizer is duplicated.
 */
public final class IncrementalJavaLexer {

    private static final int BASE_WINDOW = 256;

    private final FullRelexStrategy fullRelex = new FullRelexStrategy();
    private final JavaLexicalContextTracker tracker = new JavaLexicalContextTracker();

    /**
     * Incremental lex of {@code newText} given the previous state. Falls back
     * to a full re-lex when no previous snapshot exists.
     */
    public IncrementalLexResult lex(String oldText, LexerSnapshot previous,
                                    String newText, long newVersion) {
        String safeOld = oldText == null ? "" : oldText;
        String safeNew = newText == null ? "" : newText;
        if (previous == null) {
            return fullRelexResult(safeNew, newVersion);
        }
        TextChange change = diff(safeOld, safeNew, newVersion);
        if (change.isEmpty()) {
            return new IncrementalLexResult(
                    new LexerSnapshot(newVersion, previous.tokens()),
                    true, 0, previous.tokens().size(),
                    new TokenReuseWindow(0, 0, 0));
        }
        return incremental(safeOld, previous, safeNew, newVersion, change);
    }

    private IncrementalLexResult incremental(String oldText, LexerSnapshot previous,
                                             String newText, long newVersion, TextChange change) {
        List<Token> oldTokens = previous.tokens();
        int delta = change.delta();
        int editEndOld = change.removedRange().endOffset();

        LexicalCheckpoint checkpoint = tracker.safeCheckpointBefore(
                oldTokens, change.removedRange().startOffset(), previous.version());
        int relexStart = checkpoint.offset();
        int oldStartIndex = checkpoint.tokenIndex();

        int windowEnd = initialWindow(newText.length(), relexStart,
                change.resultingRange().endOffset());
        while (true) {
            List<Token> windowTokens = fullRelex.lexTokens(newText.substring(relexStart, windowEnd));
            Stabilization stabilized = findStabilization(
                    oldTokens, windowTokens, relexStart, oldStartIndex, delta, editEndOld);
            if (stabilized != null) {
                return assembleWithTail(oldTokens, oldStartIndex, windowTokens, relexStart,
                        stabilized, delta, newVersion);
            }
            if (windowEnd >= newText.length()) {
                return assembleFull(oldTokens, oldStartIndex, windowTokens, relexStart,
                        newVersion);
            }
            windowEnd = Math.min(newText.length(),
                    Math.max(windowEnd * 2, windowEnd + BASE_WINDOW));
        }
    }

    private IncrementalLexResult assembleWithTail(List<Token> oldTokens, int oldStartIndex,
                                                  List<Token> windowTokens, int relexStart,
                                                  Stabilization stabilized, int delta, long newVersion) {
        List<Token> result = new ArrayList<>(
                oldStartIndex + stabilized.windowIndex + (oldTokens.size() - stabilized.oldIndex));
        for (int i = 0; i < oldStartIndex; i++) {
            result.add(oldTokens.get(i));
        }
        for (int i = 0; i < stabilized.windowIndex; i++) {
            result.add(shift(windowTokens.get(i), relexStart));
        }
        int reusedTail = 0;
        for (int i = stabilized.oldIndex; i < oldTokens.size(); i++) {
            result.add(shift(oldTokens.get(i), delta));
            reusedTail++;
        }
        return new IncrementalLexResult(
                new LexerSnapshot(newVersion, result),
                true,
                windowTokens.size(),
                oldStartIndex,
                new TokenReuseWindow(stabilized.oldIndex, delta, reusedTail));
    }

    private IncrementalLexResult assembleFull(List<Token> oldTokens, int oldStartIndex,
                                              List<Token> windowTokens, int relexStart,
                                              long newVersion) {
        List<Token> result = new ArrayList<>(oldStartIndex + windowTokens.size());
        for (int i = 0; i < oldStartIndex; i++) {
            result.add(oldTokens.get(i));
        }
        for (Token token : windowTokens) {
            result.add(shift(token, relexStart));
        }
        return new IncrementalLexResult(
                new LexerSnapshot(newVersion, result),
                oldStartIndex > 0,
                windowTokens.size(),
                oldStartIndex,
                new TokenReuseWindow(0, 0, 0));
    }

    private IncrementalLexResult fullRelexResult(String newText, long newVersion) {
        LexerSnapshot snapshot = fullRelex.lex(newText, newVersion);
        return new IncrementalLexResult(snapshot, false, snapshot.tokens().size(), 0,
                new TokenReuseWindow(0, 0, 0));
    }

    /**
     * Walks the re-lexed window tokens and looks for the first token that
     * matches the previous stream (same type, text and offset mapped by the
     * delta) and starts at or after the removed region end. Such a token
     * proves both streams are aligned from there on, because the texts beyond
     * the edit are identical and the scanner is stateless.
     */
    private static Stabilization findStabilization(List<Token> oldTokens,
                                                   List<Token> windowTokens,
                                                   int relexStart,
                                                   int oldStartIndex,
                                                   int delta,
                                                   int editEndOld) {
        int oldIdx = oldStartIndex;
        for (int i = 0; i < windowTokens.size(); i++) {
            Token windowToken = windowTokens.get(i);
            long newStart = (long) relexStart + windowToken.startOffset();
            long oldMapped = newStart - delta;
            while (oldIdx < oldTokens.size() - 1
                    && oldTokens.get(oldIdx).endOffset() <= oldMapped) {
                oldIdx++;
            }
            Token oldToken = oldTokens.get(oldIdx);
            if (oldToken.startOffset() == oldMapped
                    && oldToken.type() == windowToken.type()
                    && oldToken.text().equals(windowToken.text())
                    && oldToken.startOffset() >= editEndOld) {
                return new Stabilization(i, oldIdx);
            }
        }
        return null;
    }

    private static Token shift(Token token, int delta) {
        if (delta == 0) {
            return token;
        }
        return new Token(token.type(),
                TextRange.of(token.startOffset() + delta, token.endOffset() + delta),
                token.text());
    }

    private static int initialWindow(int newLength, int relexStart, int newEditEnd) {
        int base = Math.max(relexStart + BASE_WINDOW, newEditEnd + BASE_WINDOW);
        return Math.min(newLength, base);
    }

    private static TextChange diff(String oldText, String newText, long newVersion) {
        DocumentSnapshot before = new DocumentSnapshot(newVersion - 1, oldText, null, null);
        DocumentSnapshot after = new DocumentSnapshot(newVersion, newText, null, null);
        return TextChange.between(before, after);
    }

    private record Stabilization(int windowIndex, int oldIndex) {
    }
}
