package com.eyecode.editor.intelligence.selection;

import com.eyecode.editor.intelligence.document.TextRange;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Optional;

/**
 * Bounded stack of selection states produced by smart selection expansion.
 * <p>
 * This is <strong>not</strong> an undo history: it never stores text and never
 * participates in undo/redo. It records, per document version, the selection
 * range (and its semantic level) that {@code Ctrl+W} replaced, so
 * {@code Ctrl+Shift+W} can walk back through the expansion ladder
 * ({@code R3 → R2 → R1 → collapsed}).
 * <p>
 * Entries are invalidated as soon as the document version changes: a range
 * computed for stale text is never restored.
 */
public final class SelectionHistory {

    private static final int LIMIT = 50;

    public record Entry(long version, int level, TextRange range) {
    }

    private final Deque<Entry> entries = new ArrayDeque<>();

    public void push(long version, int level, TextRange range) {
        if (!entries.isEmpty() && entries.peekFirst().version() != version) {
            entries.clear();
        }
        entries.addFirst(new Entry(version, level, range));
        while (entries.size() > LIMIT) {
            entries.removeLast();
        }
    }

    public Optional<TextRange> pop(long version) {
        if (entries.isEmpty()) {
            return Optional.empty();
        }
        if (entries.peekFirst().version() != version) {
            entries.clear();
            return Optional.empty();
        }
        return Optional.of(entries.removeFirst().range());
    }

    public Optional<Entry> top() {
        return Optional.ofNullable(entries.peekFirst());
    }

    public boolean isEmpty() {
        return entries.isEmpty();
    }

    public int size() {
        return entries.size();
    }

    public void clear() {
        entries.clear();
    }
}
