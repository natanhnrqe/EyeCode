package com.eyecode.lessons.presentation;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.document.TextChange;
import com.eyecode.language.ast.AstNodeKind;
import com.eyecode.language.ast.AstNodes;
import com.eyecode.language.java.parser.JavaParserService;

public final class JavaPresentationChangeAnalyzer {
    private static final int LOCALIZED_LIMIT = 240;

    public CodeChange analyze(String previousCode, String canonicalCode) {
        if (previousCode == null || canonicalCode == null) throw new IllegalArgumentException("Estados canônicos ausentes");
        TextChange change = TextChange.between(DocumentSnapshot.oneShot(previousCode), DocumentSnapshot.oneShot(canonicalCode));
        int start = change.removedRange().startOffset();
        int end = change.removedRange().endOffset();
        String inserted = change.insertedText();
        if (change.isEmpty()) return new CodeChange(CodeChangeKind.NO_CHANGE, start, end, inserted);
        if (previousCode.isEmpty()) return new CodeChange(CodeChangeKind.UNSAFE, start, end, inserted);
        if (change.isInsert() && isCompleteLineInsertion(previousCode, start, inserted)) {
            int lineStart = previousCode.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
            String normalizedInsertion = previousCode.substring(lineStart, start) + inserted;
            return new CodeChange(classifyLineInsertion(previousCode, canonicalCode), lineStart, start, normalizedInsertion);
        }
        if (change.isInsert() && hasLineBreak(inserted)
                && classifyLineInsertion(previousCode, canonicalCode) == CodeChangeKind.BLOCK_INSERTION) {
            return new CodeChange(CodeChangeKind.BLOCK_INSERTION, start, end, inserted);
        }
        String removed = previousCode.substring(start, end);
        if (change.isDelete() && isCompleteLineDeletion(previousCode, start, end, removed)) {
            int lineStart = previousCode.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
            int trailingIndentation = trailingIndentationAfterLineBreak(removed);
            return new CodeChange(classifyLineDeletion(previousCode, canonicalCode), lineStart, end - trailingIndentation, "");
        }
        if (inserted.length() + removed.length() > LOCALIZED_LIMIT || hasLineBreak(inserted) || hasLineBreak(removed)) {
            return new CodeChange(CodeChangeKind.UNSAFE, start, end, inserted);
        }
        if (change.isReplace() && isSignificantLineReplacement(previousCode, canonicalCode, start, end, inserted)) {
            return wholeLineReplacement(previousCode, canonicalCode, start, end);
        }
        CodeChangeKind kind = change.isInsert() ? CodeChangeKind.LOCALIZED_INSERT
                : change.isDelete() ? CodeChangeKind.LOCALIZED_DELETE : CodeChangeKind.LOCALIZED_REPLACE;
        return new CodeChange(kind, start, end, inserted);
    }

    private static boolean isCompleteLineInsertion(String previousCode, int offset, String inserted) {
        int lineStart = previousCode.lastIndexOf('\n', Math.max(0, offset - 1)) + 1;
        if (!previousCode.substring(lineStart, offset).isBlank()) return false;
        int newline = inserted.indexOf('\n');
        if (newline < 0 || inserted.indexOf('\n', newline + 1) >= 0) return false;
        String beforeLineBreak = inserted.substring(0, newline > 0 && inserted.charAt(newline - 1) == '\r' ? newline - 1 : newline);
        String afterLineBreak = inserted.substring(newline + 1);
        return !beforeLineBreak.isBlank() && afterLineBreak.isBlank();
    }

    private static CodeChangeKind classifyLineInsertion(String previousCode, String canonicalCode) {
        try {
            var parser = new JavaParserService();
            var before = parser.parse(DocumentSnapshot.oneShot(previousCode)).astRoot();
            var after = parser.parse(DocumentSnapshot.oneShot(canonicalCode)).astRoot();
            long beforeStatements = AstNodes.descendants(before).stream().filter(node -> isStatement(node.kind())).count();
            long afterStatements = AstNodes.descendants(after).stream().filter(node -> isStatement(node.kind())).count();
            long beforeBlocks = AstNodes.descendants(before).stream().filter(node -> node.kind() == AstNodeKind.BLOCK).count();
            long afterBlocks = AstNodes.descendants(after).stream().filter(node -> node.kind() == AstNodeKind.BLOCK).count();
            if (afterBlocks > beforeBlocks) return CodeChangeKind.BLOCK_INSERTION;
            if (afterStatements > beforeStatements) return CodeChangeKind.STATEMENT_INSERTION;
        } catch (RuntimeException ignored) {
            return CodeChangeKind.UNSAFE;
        }
        return CodeChangeKind.UNSAFE;
    }

    private static boolean isCompleteLineDeletion(String previousCode, int start, int end, String removed) {
        int lineStart = previousCode.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        if (!previousCode.substring(lineStart, start).isBlank()) return false;
        int newline = removed.indexOf('\n');
        if (newline < 0 || removed.indexOf('\n', newline + 1) >= 0) return false;
        return !removed.substring(0, newline).isBlank()
                && removed.substring(newline + 1).isBlank()
                && end < previousCode.length() && previousCode.charAt(end) == '}';
    }

    private static int trailingIndentationAfterLineBreak(String removed) {
        int newline = removed.indexOf('\n');
        return removed.length() - newline - 1;
    }

    private static CodeChangeKind classifyLineDeletion(String previousCode, String canonicalCode) {
        try {
            var parser = new JavaParserService();
            var before = parser.parse(DocumentSnapshot.oneShot(previousCode)).astRoot();
            var after = parser.parse(DocumentSnapshot.oneShot(canonicalCode)).astRoot();
            long beforeStatements = AstNodes.descendants(before).stream().filter(node -> isStatement(node.kind())).count();
            long afterStatements = AstNodes.descendants(after).stream().filter(node -> isStatement(node.kind())).count();
            long beforeBlocks = AstNodes.descendants(before).stream().filter(node -> node.kind() == AstNodeKind.BLOCK).count();
            long afterBlocks = AstNodes.descendants(after).stream().filter(node -> node.kind() == AstNodeKind.BLOCK).count();
            if (beforeBlocks == afterBlocks && beforeStatements > afterStatements) return CodeChangeKind.STATEMENT_DELETION;
        } catch (RuntimeException ignored) {
            return CodeChangeKind.UNSAFE;
        }
        return CodeChangeKind.UNSAFE;
    }

    private static boolean isSignificantLineReplacement(String previousCode, String canonicalCode,
                                                        int start, int end, String inserted) {
        int oldLineStart = previousCode.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        int oldLineEnd = lineEnd(previousCode, end);
        int newLineStart = canonicalCode.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        int newLineEnd = lineEnd(canonicalCode, start + inserted.length());
        String oldLine = previousCode.substring(oldLineStart, oldLineEnd).trim();
        String newLine = canonicalCode.substring(newLineStart, newLineEnd).trim();
        if (oldLine.isEmpty() || newLine.isEmpty()) return false;
        int preserved = commonPrefix(oldLine, newLine) + commonSuffix(oldLine, newLine);
        return preserved * 2 < Math.max(oldLine.length(), newLine.length());
    }

    private static CodeChange wholeLineReplacement(String previousCode, String canonicalCode, int start, int end) {
        int oldLineStart = previousCode.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        int oldLineEnd = lineEnd(previousCode, end);
        int newLineStart = canonicalCode.lastIndexOf('\n', Math.max(0, start - 1)) + 1;
        int newLineEnd = lineEnd(canonicalCode, start);
        return new CodeChange(CodeChangeKind.LINE_REPLACE, oldLineStart, oldLineEnd,
                canonicalCode.substring(newLineStart, newLineEnd));
    }

    private static int lineEnd(String source, int offset) {
        int newline = source.indexOf('\n', offset);
        int end = newline < 0 ? source.length() : newline;
        return end > 0 && source.charAt(end - 1) == '\r' ? end - 1 : end;
    }

    private static int commonPrefix(String first, String second) {
        int index = 0;
        int limit = Math.min(first.length(), second.length());
        while (index < limit && first.charAt(index) == second.charAt(index)) index++;
        return index;
    }

    private static int commonSuffix(String first, String second) {
        int index = 0;
        int limit = Math.min(first.length(), second.length());
        while (index < limit && first.charAt(first.length() - index - 1) == second.charAt(second.length() - index - 1)) index++;
        return index;
    }

    private static boolean isStatement(AstNodeKind kind) {
        return switch (kind) {
            case LOCAL_VARIABLE_DECLARATION, EXPRESSION_STATEMENT, IF_STATEMENT, FOR_STATEMENT,
                    ENHANCED_FOR_STATEMENT, WHILE_STATEMENT, DO_WHILE_STATEMENT, RETURN_STATEMENT,
                    BREAK_STATEMENT, CONTINUE_STATEMENT, THROW_STATEMENT, TRY_STATEMENT, SWITCH_STATEMENT,
                    SYNCHRONIZED_STATEMENT, LABELED_STATEMENT, YIELD_STATEMENT, ASSERT_STATEMENT -> true;
            default -> false;
        };
    }

    private static boolean hasLineBreak(String text) {
        return text.indexOf('\n') >= 0 || text.indexOf('\r') >= 0;
    }
}
