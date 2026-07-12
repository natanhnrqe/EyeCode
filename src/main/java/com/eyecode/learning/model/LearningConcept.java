package com.eyecode.learning.model;

import com.eyecode.learning.content.LearningPage;

import java.util.List;

public final class LearningConcept {

    private String id;
    private String title;
    private String description;
    private ConceptType type;
    private DifficultyLevel difficulty;
    private List<String> relatedConcepts;
    private LearningPage page;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public ConceptType getType() {
        return type;
    }

    public void setType(ConceptType type) {
        this.type = type;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public List<String> getRelatedConcepts() {
        return relatedConcepts;
    }

    public void setRelatedConcepts(List<String> relatedConcepts) {
        this.relatedConcepts = relatedConcepts;
    }

    public LearningPage getPage() {
        return page;
    }

    public void setPage(LearningPage page) {
        this.page = page;
    }
}
