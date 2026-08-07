package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.command.CommandManager;

/**
 * Context handed to an {@link EditorCommand} when it executes.
 * <p>
 * Exposes only Core abstractions — never UI types. All reads happen through
 * the immutable {@link DocumentSnapshot}; all writes go through the
 * {@link EditorBuffer} or a {@code DocumentTransaction}.
 */
public interface EditorCommandContext {

    EditorBuffer buffer();

    EditorDocument document();

    DocumentSnapshot snapshot();

    CommandManager commandManager();
}
