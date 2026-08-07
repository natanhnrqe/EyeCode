package com.eyecode.editor.intelligence.document;

final class ImmutableLineMap implements LineMap {

    private final int textLength;
    private final int[] lineStarts;
    private final int[] lineEnds;

    ImmutableLineMap(CharSequence text) {
        CharSequence source = text == null ? "" : text;
        this.textLength = source.length();

        int[] starts = new int[source.length() / 2 + 2];
        int[] ends = new int[source.length() / 2 + 2];
        int startCount = 1;
        starts[0] = 0;

        int i = 0;
        int endCount = 0;
        while (i < source.length()) {
            char c = source.charAt(i);
            if (c == '\r') {
                int terminatorLength = (i + 1 < source.length() && source.charAt(i + 1) == '\n') ? 2 : 1;
                ends[endCount++] = i;
                starts[startCount++] = i + terminatorLength;
                i += terminatorLength;
            } else if (c == '\n' || c == '\u000B' || c == '\u000C' || c == '\u0085' || c == '\u2028' || c == '\u2029') {
                ends[endCount++] = i;
                starts[startCount++] = i + 1;
                i++;
            } else {
                i++;
            }
        }
        ends[endCount++] = source.length();

        this.lineStarts = new int[startCount];
        this.lineEnds = new int[endCount];
        System.arraycopy(starts, 0, lineStarts, 0, startCount);
        System.arraycopy(ends, 0, lineEnds, 0, endCount);
    }

    @Override
    public int lineCount() {
        return lineStarts.length;
    }

    @Override
    public int lineStartOffset(int line) {
        checkLine(line);
        return lineStarts[line];
    }

    @Override
    public int lineEndOffset(int line) {
        checkLine(line);
        return lineEnds[line];
    }

    @Override
    public int lineOfOffset(int offset) {
        int safe = clampOffset(offset);
        int low = 0;
        int high = lineStarts.length - 1;
        while (low < high) {
            int mid = (low + high + 1) >>> 1;
            if (lineStarts[mid] <= safe) {
                low = mid;
            } else {
                high = mid - 1;
            }
        }
        return low;
    }

    @Override
    public int columnOfOffset(int offset) {
        return offset - lineStarts[lineOfOffset(offset)];
    }

    @Override
    public int offsetOf(int line, int column) {
        int safeLine = Math.max(0, Math.min(line, lineCount() - 1));
        int contentLength = lineEnds[safeLine] - lineStarts[safeLine];
        int safeColumn = Math.max(0, Math.min(column, contentLength));
        return lineStarts[safeLine] + safeColumn;
    }

    private int clampOffset(int offset) {
        if (offset < 0) return 0;
        if (offset > textLength) return textLength;
        return offset;
    }

    private void checkLine(int line) {
        if (line < 0 || line >= lineStarts.length) {
            throw new IndexOutOfBoundsException("Line out of range: " + line);
        }
    }
}
