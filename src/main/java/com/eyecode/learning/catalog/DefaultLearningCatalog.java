package com.eyecode.learning.catalog;

import com.eyecode.learning.content.LearningContentSection;
import com.eyecode.learning.content.LearningContentType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class DefaultLearningCatalog implements LearningCatalog {

    private final Map<ConceptType, LearningConcept> concepts;

    public DefaultLearningCatalog() {
        this.concepts = new EnumMap<>(ConceptType.class);
        register(ConceptType.CLASS, "class", "Class",
                "A Java class defines a blueprint for creating objects.",
                DifficultyLevel.BEGINNER);
        concepts.get(ConceptType.CLASS).setPage(buildClassPage());
        register(ConceptType.INTERFACE, "interface", "Interface",
                "A Java interface defines a contract that implementing classes must fulfill.",
                DifficultyLevel.INTERMEDIATE);
        register(ConceptType.ENUM, "enum", "Enum",
                "A Java enum defines a fixed set of named constants.",
                DifficultyLevel.BEGINNER);
        register(ConceptType.RECORD, "record", "Record",
                "A Java record is a concise way to define immutable data carriers.",
                DifficultyLevel.INTERMEDIATE);
    }

    private void register(ConceptType type, String id, String title,
                          String description, DifficultyLevel difficulty) {
        LearningConcept concept = new LearningConcept();
        concept.setId(id);
        concept.setTitle(title);
        concept.setDescription(description);
        concept.setType(type);
        concept.setDifficulty(difficulty);
        concept.setRelatedConcepts(Collections.emptyList());
        concepts.put(type, concept);
    }

    @Override
    public LearningConcept get(ConceptType type) {
        return concepts.get(type);
    }

    @Override
    public boolean contains(ConceptType type) {
        return concepts.containsKey(type);
    }

    private static LearningPage buildClassPage() {
        List<LearningContentSection> sections = new ArrayList<>();
        sections.add(section("intro",         "O que é uma classe?",          LearningContentType.INTRODUCTION,        1));
        sections.add(section("analogy",       "Analogia",                     LearningContentType.ANALOGY,             2));
        sections.add(section("real-world",    "Exemplo do mundo real",        LearningContentType.REAL_WORLD_EXAMPLE,  3));
        sections.add(section("code-example",  "Exemplo em Java",              LearningContentType.CODE_EXAMPLE,        4));
        sections.add(section("how-it-works",  "Como funciona internamente",   LearningContentType.HOW_IT_WORKS,        5));
        sections.add(section("mistakes",      "Erros comuns",                 LearningContentType.COMMON_MISTAKES,     6));
        sections.add(section("next-step",     "Próximo passo",                LearningContentType.NEXT_STEP,           7));
        sections.add(section("reference",     "Documentação oficial",         LearningContentType.TECHNICAL_REFERENCE, 8));

        LearningPage page = new LearningPage();
        page.setId("class-page");
        page.setTitle("Classes em Java");
        page.setShortDescription("Classes são modelos utilizados para criar objetos em Java.");
        page.setDifficulty(DifficultyLevel.BEGINNER);
        page.setSections(sections);
        return page;
    }

    private static LearningContentSection section(String id, String title,
                                                  LearningContentType type, int order) {
        LearningContentSection s = new LearningContentSection();
        s.setId(id);
        s.setTitle(title);
        s.setContent("Conteúdo será implementado na próxima sprint.");
        s.setType(type);
        s.setOrder(order);
        return s;
    }
}
