package com.eyecode.workbench.editor;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.language.semantic.DefinitionLocation;

import java.util.Optional;

public interface EditorIntelligence extends AutoCloseable {
    void activated(DocumentSnapshot document);

    void deactivated(DocumentSnapshot document);

    void closed(DocumentSnapshot document);

    Optional<DefinitionLocation> resolveDefinition(DocumentSnapshot document, int caretOffset);

    @Override
    void close();
}
