package com.eyecode.editor.v2.completion.insert;

import com.eyecode.editor.intelligence.document.LineMap;

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
        return LineMap.of(text).offsetOf(caret.line(), caret.column());
    }
}
