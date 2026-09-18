package com.eyecode.lessons.presentation;

import java.util.List;
import java.util.ArrayList;

public final class PresentationTransitionPlanner {
    public List<PresentationOperation> plan(String source, String target, CodeChange change, boolean instant) {
        return switch (change.kind()) {
            case NO_CHANGE -> List.of();
            case LOCALIZED_INSERT -> List.of(PresentationOperation.edit(PresentationOperationType.TYPE_TEXT,
                    change.startOffset(), change.endOffset(), change.insertedText()));
            case LOCALIZED_DELETE -> List.of(PresentationOperation.edit(PresentationOperationType.DELETE_TEXT,
                    change.startOffset(), change.endOffset(), ""));
            case LOCALIZED_REPLACE -> List.of(PresentationOperation.edit(PresentationOperationType.REPLACE_TEXT,
                    change.startOffset(), change.endOffset(), change.insertedText()));
            case LINE_REPLACE -> List.of(PresentationOperation.edit(PresentationOperationType.REPLACE_TEXT,
                    change.startOffset(), change.endOffset(), change.insertedText()));
            case STATEMENT_INSERTION -> List.of(lineTyping(change));
            case STATEMENT_DELETION -> List.of(PresentationOperation.edit(PresentationOperationType.DELETE_TEXT,
                    change.startOffset(), change.endOffset(), ""));
            case BLOCK_INSERTION, UNSAFE -> List.of(genericReplacement(source, target));
        };
    }

    private static PresentationOperation genericReplacement(String source, String target) {
        List<String> sourceLines = lines(source);
        List<String> targetLines = lines(target);
        int prefix = 0;
        while (prefix < sourceLines.size() && prefix < targetLines.size()
                && sourceLines.get(prefix).equals(targetLines.get(prefix))) prefix++;
        int suffix = 0;
        while (suffix < sourceLines.size() - prefix && suffix < targetLines.size() - prefix
                && sourceLines.get(sourceLines.size() - suffix - 1).equals(targetLines.get(targetLines.size() - suffix - 1))) suffix++;
        int start = length(sourceLines, 0, prefix);
        int end = source.length() - length(sourceLines, sourceLines.size() - suffix, sourceLines.size());
        String replacement = join(targetLines, prefix, targetLines.size() - suffix);
        return PresentationOperation.edit(PresentationOperationType.REPLACE_TEXT, start, end, replacement);
    }

    private static List<String> lines(String source) {
        List<String> lines = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < source.length(); index++) {
            if (source.charAt(index) == '\n') {
                lines.add(source.substring(start, index + 1));
                start = index + 1;
            }
        }
        if (start < source.length()) lines.add(source.substring(start));
        return lines;
    }

    private static int length(List<String> lines, int start, int end) {
        int length = 0;
        for (int index = start; index < end; index++) length += lines.get(index).length();
        return length;
    }

    private static String join(List<String> lines, int start, int end) {
        StringBuilder text = new StringBuilder();
        for (int index = start; index < end; index++) text.append(lines.get(index));
        return text.toString();
    }

    private static PresentationOperation lineTyping(CodeChange change) {
        String inserted = change.insertedText();
        int newline = inserted.indexOf('\n');
        int lineEnd = newline > 0 && inserted.charAt(newline - 1) == '\r' ? newline - 1 : newline;
        String line = inserted.substring(0, lineEnd);
        String suffix = inserted.substring(lineEnd);
        int contentStart = 0;
        while (contentStart < line.length() && (line.charAt(contentStart) == ' ' || line.charAt(contentStart) == '\t')) contentStart++;
        return PresentationOperation.typeLine(change.startOffset(), change.endOffset(), line.substring(0, contentStart),
                line.substring(contentStart), suffix);
    }
}
