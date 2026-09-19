package com.eyecode.language.java;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.eventbus.EventBus;
import com.eyecode.language.semantic.DefinitionAtCaretResolver;
import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.language.symbol.DocumentSemanticModelBuilder;
import com.eyecode.language.symbol.SemanticModelSnapshot;
import com.eyecode.workbench.editor.EditorIntelligence;

import java.util.Optional;

public final class JavaEditorIntelligence implements EditorIntelligence {
    private final JavaLexerService lexerService;
    private final DocumentSemanticModelBuilder semanticModelBuilder;
    private final DefinitionAtCaretResolver definitionResolver;
    private final LexerEventBridge lexerEventBridge;

    public JavaEditorIntelligence(EventBus eventBus) {
        lexerService = new JavaLexerService();
        semanticModelBuilder = new DocumentSemanticModelBuilder(lexerService);
        definitionResolver = new DefinitionAtCaretResolver();
        lexerEventBridge = eventBus == null ? null : new LexerEventBridge(lexerService, eventBus);
    }

    @Override
    public void activated(DocumentSnapshot document) {
        if (document != null) lexerService.activateSession(document.sessionId());
    }

    @Override
    public void deactivated(DocumentSnapshot document) {
        if (document != null) lexerService.deactivateSession(document.sessionId());
    }

    @Override
    public void closed(DocumentSnapshot document) {
        if (document != null) lexerService.invalidateSession(document.sessionId());
    }

    @Override
    public Optional<DefinitionLocation> resolveDefinition(DocumentSnapshot document, int caretOffset) {
        if (document == null || caretOffset < 0 || caretOffset > document.length()) return Optional.empty();
        Optional<SemanticModelSnapshot> model = semanticModelBuilder.build(document);
        return model.flatMap(value -> definitionResolver.resolve(document.getText(), caretOffset, value.symbolTable()));
    }

    @Override
    public void close() {
        if (lexerEventBridge != null) lexerEventBridge.dispose();
    }
}
