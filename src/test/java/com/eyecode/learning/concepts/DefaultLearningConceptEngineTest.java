package com.eyecode.learning.concepts;

import com.eyecode.editor.v2.language.java.symbols.ProjectSymbol;
import com.eyecode.editor.v2.language.java.symbols.SymbolKind;
import com.eyecode.learning.analysis.LearningAnalysisContext;
import com.eyecode.learning.analysis.LearningContextResolver;
import com.eyecode.learning.concepts.providers.ClassConceptProvider;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.model.LearningContext;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DefaultLearningConceptEngineTest {

    private final ClassConceptProvider classProvider = new ClassConceptProvider();

    @Test
    void classKindReturnsClassConcept() {
        assertConceptForKind(SymbolKind.CLASS, ConceptType.CLASS, DifficultyLevel.BEGINNER);
    }

    @Test
    void interfaceKindReturnsInterfaceConcept() {
        assertConceptForKind(SymbolKind.INTERFACE, ConceptType.INTERFACE, DifficultyLevel.INTERMEDIATE);
    }

    @Test
    void enumKindReturnsEnumConcept() {
        assertConceptForKind(SymbolKind.ENUM, ConceptType.ENUM, DifficultyLevel.BEGINNER);
    }

    @Test
    void recordKindReturnsRecordConcept() {
        assertConceptForKind(SymbolKind.RECORD, ConceptType.RECORD, DifficultyLevel.INTERMEDIATE);
    }

    @Test
    void methodKindReturnsEmptyList() {
        assertEmptyForKind(SymbolKind.METHOD);
    }

    @Test
    void fieldKindReturnsEmptyList() {
        assertEmptyForKind(SymbolKind.FIELD);
    }

    @Test
    void nullSymbolReturnsEmptyList() {
        LearningContext ctx = new LearningContext();
        ctx.setCurrentSymbol(null);

        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of(classProvider));
        List<LearningConcept> result = engine.analyze(ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void nullContextReturnsEmptyList() {
        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of(classProvider));
        List<LearningConcept> result = engine.analyze(null);

        assertTrue(result.isEmpty());
    }

    @Test
    void engineAggregatesMultipleProviders() {
        LearningConceptProvider fakeProvider = ctx -> List.of(createConcept("fake", ConceptType.LOOP, DifficultyLevel.ADVANCED));

        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of(classProvider, fakeProvider));

        LearningContext ctx = contextWithSymbol(SymbolKind.ENUM);
        List<LearningConcept> result = engine.analyze(ctx);

        assertEquals(2, result.size());
    }

    @Test
    void engineWithNullProvidersIsSafe() {
        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(null);

        LearningContext ctx = contextWithSymbol(SymbolKind.CLASS);
        List<LearningConcept> result = engine.analyze(ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void engineWithEmptyProvidersIsSafe() {
        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of());

        LearningContext ctx = contextWithSymbol(SymbolKind.CLASS);
        List<LearningConcept> result = engine.analyze(ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void resolverCanBeInjected() {
        LearningAnalysisContext expected = new LearningAnalysisContext(null, null, null, null, null, null);
        LearningContextResolver custom = ctx -> expected;

        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of(), custom);

        LearningContext ctx = contextWithSymbol(SymbolKind.CLASS);
        LearningConceptProvider spy = actualCtx -> {
            assertSame(expected, actualCtx);
            return List.of();
        };

        DefaultLearningConceptEngine engineWithSpy = new DefaultLearningConceptEngine(List.of(spy), custom);
        List<LearningConcept> result = engineWithSpy.analyze(ctx);

        assertTrue(result.isEmpty());
    }

    @Test
    void nullLearningContextReturnsNullAnalysisContext() {
        LearningContextResolver custom = ctx -> null;

        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of(classProvider), custom);

        LearningContext ctx = contextWithSymbol(SymbolKind.CLASS);
        List<LearningConcept> result = engine.analyze(ctx);

        assertTrue(result.isEmpty());
    }

    private void assertConceptForKind(SymbolKind kind, ConceptType expectedType, DifficultyLevel expectedDifficulty) {
        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of(classProvider));

        LearningContext ctx = contextWithSymbol(kind);
        List<LearningConcept> result = engine.analyze(ctx);

        assertEquals(1, result.size());

        LearningConcept concept = result.getFirst();
        assertNotNull(concept.getId());
        assertEquals(expectedType.name(), concept.getTitle());
        assertEquals(expectedType, concept.getType());
        assertEquals(expectedDifficulty, concept.getDifficulty());
        assertNotNull(concept.getDescription());
    }

    private void assertEmptyForKind(SymbolKind kind) {
        DefaultLearningConceptEngine engine = new DefaultLearningConceptEngine(List.of(classProvider));

        LearningContext ctx = contextWithSymbol(kind);
        List<LearningConcept> result = engine.analyze(ctx);

        assertTrue(result.isEmpty());
    }

    private static LearningContext contextWithSymbol(SymbolKind kind) {
        ProjectSymbol symbol = new ProjectSymbol();
        symbol.setKind(kind);
        symbol.setName(kind.name().toLowerCase());

        LearningContext ctx = new LearningContext();
        ctx.setCurrentSymbol(symbol);
        return ctx;
    }

    private static LearningConcept createConcept(String id, ConceptType type, DifficultyLevel difficulty) {
        LearningConcept c = new LearningConcept();
        c.setId(id);
        c.setTitle(type.name());
        c.setType(type);
        c.setDifficulty(difficulty);
        return c;
    }
}
