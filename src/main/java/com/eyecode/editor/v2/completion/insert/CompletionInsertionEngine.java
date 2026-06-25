package com.eyecode.editor.v2.completion.insert;

public final class CompletionInsertionEngine {

    public void insert(CompletionInsertionContext context) {
        String prefix = context.getCurrentPrefix();
        String insertText = context.getItem().getInsertText();
        int caretOffset = toOffset(context.getDocument().getText(), context.getCaret());
        int start = Math.max(0, caretOffset - prefix.length());
        int end = caretOffset;

        if (end > start) {
            context.getDocument().delete(start, end);
        }
        context.getDocument().insert(start, insertText);
    }

    private int toOffset(String text, com.eyecode.editor.v2.EditorPosition caret) {
        int line = 0;
        int column = 0;
        for (int offset = 0; offset < text.length(); offset++) {
            if (line == caret.line() && column == caret.column()) {
                return offset;
            }
            char current = text.charAt(offset);
            if (current == '\n') {
                line++;
                column = 0;
            } else {
                column++;
            }
        }
        return text.length();
    }
}
