package com.eyecode.editor.v2.completion.insert;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.EditorPosition;
import com.eyecode.editor.v2.completion.CompletionItem;

import java.util.Objects;

public final class CompletionInsertionContext {

    private final EditorDocument document;
    private final EditorPosition caret;
    private final CompletionItem item;
    private final String currentPrefix;

    public CompletionInsertionContext(EditorDocument document, EditorPosition caret,
                                      CompletionItem item, String currentPrefix) {
        this.document = Objects.requireNonNull(document);
        this.caret = Objects.requireNonNull(caret);
        this.item = Objects.requireNonNull(item);
        this.currentPrefix = currentPrefix == null ? "" : currentPrefix;
    }

    public EditorDocument getDocument() { return document; }

    public EditorPosition getCaret() { return caret; }

    public CompletionItem getItem() { return item; }

    public String getCurrentPrefix() { return currentPrefix; }
}
