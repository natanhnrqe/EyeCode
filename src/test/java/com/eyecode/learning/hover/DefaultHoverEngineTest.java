package com.eyecode.learning.hover;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SymbolKind;
import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.catalog.DefaultLearningCatalog;
import com.eyecode.learning.catalog.LearningCatalog;
import com.eyecode.learning.concepts.DefaultLearningConceptEngine;
import com.eyecode.learning.concepts.LearningConceptEngine;
import com.eyecode.learning.concepts.providers.ClassConceptProvider;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class DefaultHoverEngineTest {

    private final LearningCatalog catalog = new DefaultLearningCatalog();
    private final ClassConceptProvider classProvider = new ClassConceptProvider(catalog);
    private final LearningConceptEngine conceptEngine = new DefaultLearningConceptEngine(List.of(classProvider));
    private final ConceptHoverProvider conceptHover = new ConceptHoverProvider(conceptEngine);

    @Test
    void hoverReturnsConceptForClass() {
        Optional<LearningConcept> result = hoverForKind(SymbolKind.CLASS);
        assertTrue(result.isPresent());
        assertEquals(ConceptType.CLASS, result.get().getType());
        assertEquals(DifficultyLevel.BEGINNER, result.get().getDifficulty());
    }

    @Test
    void hoverReturnsConceptForInterface() {
        Optional<LearningConcept> result = hoverForKind(SymbolKind.INTERFACE);
        assertTrue(result.isPresent());
        assertEquals(ConceptType.INTERFACE, result.get().getType());
        assertEquals(DifficultyLevel.INTERMEDIATE, result.get().getDifficulty());
    }

    @Test
    void hoverReturnsConceptForEnum() {
        Optional<LearningConcept> result = hoverForKind(SymbolKind.ENUM);
        assertTrue(result.isPresent());
        assertEquals(ConceptType.ENUM, result.get().getType());
        assertEquals(DifficultyLevel.BEGINNER, result.get().getDifficulty());
    }

    @Test
    void hoverReturnsConceptForRecord() {
        Optional<LearningConcept> result = hoverForKind(SymbolKind.RECORD);
        assertTrue(result.isPresent());
        assertEquals(ConceptType.RECORD, result.get().getType());
        assertEquals(DifficultyLevel.INTERMEDIATE, result.get().getDifficulty());
    }

    @Test
    void hoverReturnsEmptyForMethod() {
        Optional<LearningConcept> result = hoverForKind(SymbolKind.METHOD);
        assertTrue(result.isEmpty());
    }

    @Test
    void hoverReturnsEmptyForField() {
        Optional<LearningConcept> result = hoverForKind(SymbolKind.FIELD);
        assertTrue(result.isEmpty());
    }

    @Test
    void hoverReturnsEmptyForNullContext() {
        DefaultHoverEngine engine = new DefaultHoverEngine(List.of(conceptHover));
        Optional<LearningConcept> result = engine.resolve(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void hoverReturnsEmptyWhenNoProviderResponds() {
        HoverProvider emptyProvider = ctx -> Optional.empty();
        DefaultHoverEngine engine = new DefaultHoverEngine(List.of(emptyProvider));

        LearningAnalysisContext ctx = analysisContextWithSymbol(SymbolKind.CLASS);
        Optional<LearningConcept> result = engine.resolve(ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void hoverUsesFirstPresentResult() {
        HoverProvider first = ctx -> Optional.of(concept("first", ConceptType.CLASS));
        HoverProvider second = ctx -> Optional.of(concept("second", ConceptType.INTERFACE));
        DefaultHoverEngine engine = new DefaultHoverEngine(List.of(first, second));

        LearningAnalysisContext ctx = analysisContextWithSymbol(SymbolKind.CLASS);
        Optional<LearningConcept> result = engine.resolve(ctx);

        assertTrue(result.isPresent());
        assertEquals("first", result.get().getId());
    }

    @Test
    void hoverSkipsEmptyProviders() {
        HoverProvider empty = ctx -> Optional.empty();
        HoverProvider second = ctx -> Optional.of(concept("found", ConceptType.ENUM));
        DefaultHoverEngine engine = new DefaultHoverEngine(List.of(empty, second));

        LearningAnalysisContext ctx = analysisContextWithSymbol(SymbolKind.CLASS);
        Optional<LearningConcept> result = engine.resolve(ctx);

        assertTrue(result.isPresent());
        assertEquals("found", result.get().getId());
    }

    @Test
    void hoverReturnsEmptyWithNullProviderResult() {
        HoverProvider nullProvider = ctx -> null;
        DefaultHoverEngine engine = new DefaultHoverEngine(List.of(nullProvider));

        LearningAnalysisContext ctx = analysisContextWithSymbol(SymbolKind.CLASS);
        Optional<LearningConcept> result = engine.resolve(ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void hoverWithNullProvidersIsSafe() {
        DefaultHoverEngine engine = new DefaultHoverEngine(null);

        LearningAnalysisContext ctx = analysisContextWithSymbol(SymbolKind.CLASS);
        Optional<LearningConcept> result = engine.resolve(ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void conceptHoverProviderWithNullContextReturnsEmpty() {
        ConceptHoverProvider provider = new ConceptHoverProvider(conceptEngine);
        Optional<LearningConcept> result = provider.getHover(null);
        assertTrue(result.isEmpty());
    }

    @Test
    void conceptHoverProviderWithNullLearningContextReturnsEmpty() {
        LearningAnalysisContext ctx = new LearningAnalysisContext(null, null, null, null, null, null);
        ConceptHoverProvider provider = new ConceptHoverProvider(conceptEngine);
        Optional<LearningConcept> result = provider.getHover(ctx);
        assertTrue(result.isEmpty());
    }

    private Optional<LearningConcept> hoverForKind(SymbolKind kind) {
        ConceptHoverProvider provider = new ConceptHoverProvider(conceptEngine);
        DefaultHoverEngine engine = new DefaultHoverEngine(List.of(provider));

        LearningAnalysisContext ctx = analysisContextWithSymbol(kind);
        return engine.resolve(ctx);
    }

    private static LearningAnalysisContext analysisContextWithSymbol(SymbolKind kind) {
        ProjectSymbol symbol = new ProjectSymbol();
        symbol.setKind(kind);
        symbol.setName(kind.name().toLowerCase());

        LearningContext lc = new LearningContext();
        lc.setCurrentSymbol(symbol);

        return new LearningAnalysisContext(lc, symbol, null, null, null, null);
    }

    private static LearningConcept concept(String id, ConceptType type) {
        LearningConcept c = new LearningConcept();
        c.setId(id);
        c.setTitle(type.name());
        c.setType(type);
        return c;
    }
}
