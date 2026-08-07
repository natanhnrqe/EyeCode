package com.eyecode.editor.intelligence.document;

/**
 * Immutable line index for a piece of text.
 * <p>
 * Provides O(log n) conversion between char offsets and line/column coordinates.
 * A line terminator sequence (as matched by the regular expression {@code \R})
 * does not belong to any line; the line end offset is exclusive of the terminator.
 */
public interface LineMap {

    int lineCount();

    int lineStartOffset(int line);

    int lineEndOffset(int line);

    int lineOfOffset(int offset);

    int columnOfOffset(int offset);

    int offsetOf(int line, int column);

    static LineMap of(CharSequence text) {
        return new ImmutableLineMap(text);
    }

    static LineMap empty() {
        return new ImmutableLineMap("");
    }
}
