package com.eyecode.editor.intelligence.document;

/**
 * Describes a single, atomic text mutation.
 * <p>
 * {@code removedRange} addresses the text state before the change;
 * {@code insertedText} is what replaced it; {@code resultingRange} addresses
 * the text state after the change.
 */
public final class TextChange {

    private final long documentVersion;
    private final TextRange removedRange;
    private final String insertedText;
    private final TextRange resultingRange;

    public TextChange(long documentVersion,
                      TextRange removedRange,
                      String insertedText,
                      TextRange resultingRange) {
        this.documentVersion = documentVersion;
        this.removedRange = removedRange == null ? new TextRange(0, 0) : removedRange;
        this.insertedText = insertedText == null ? "" : insertedText;
        this.resultingRange = resultingRange != null ? resultingRange : this.removedRange;
    }

    public long documentVersion() {
        return documentVersion;
    }

    public TextRange removedRange() {
        return removedRange;
    }

    public String insertedText() {
        return insertedText;
    }

    public TextRange resultingRange() {
        return resultingRange;
    }

    public boolean isInsert() {
        return removedRange.isEmpty() && !insertedText.isEmpty();
    }

    public boolean isDelete() {
        return !removedRange.isEmpty() && insertedText.isEmpty();
    }

    public boolean isReplace() {
        return !removedRange.isEmpty() && !insertedText.isEmpty();
    }

    public boolean isEmpty() {
        return removedRange.isEmpty() && insertedText.isEmpty();
    }

    public int delta() {
        return insertedText.length() - removedRange.length();
    }

    public static TextChange between(DocumentSnapshot before, DocumentSnapshot after) {
        if (before == null || after == null) {
            throw new IllegalArgumentException("Snapshots must not be null");
        }
        String beforeText = before.getText();
        String afterText = after.getText();

        int prefix = commonPrefix(beforeText, afterText);
        int suffix = commonSuffix(beforeText, afterText, prefix);

        int oldEnd = beforeText.length() - suffix;
        int newEnd = afterText.length() - suffix;

        String inserted = afterText.substring(prefix, newEnd);
        TextRange removed = new TextRange(prefix, oldEnd);
        TextRange resulting = new TextRange(prefix, prefix + inserted.length());

        return new TextChange(after.version(), removed, inserted, resulting);
    }

    private static int commonPrefix(String a, String b) {
        int max = Math.min(a.length(), b.length());
        int i = 0;
        while (i < max && a.charAt(i) == b.charAt(i)) {
            i++;
        }
        return i;
    }

    private static int commonSuffix(String a, String b, int prefix) {
        int max = Math.min(a.length(), b.length()) - prefix;
        int suffix = 0;
        while (suffix < max
                && a.charAt(a.length() - 1 - suffix) == b.charAt(b.length() - 1 - suffix)) {
            suffix++;
        }
        return suffix;
    }
}
