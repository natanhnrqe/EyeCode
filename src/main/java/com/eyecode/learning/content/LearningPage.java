package com.eyecode.learning.content;

import com.eyecode.learning.model.DifficultyLevel;

import java.util.List;

public final class LearningPage {

    private final String resourcePath;
    private String id;
    private String title;
    private String shortDescription;
    private DifficultyLevel difficulty;
    private List<LearningContentSection> sections;

    public LearningPage() {
        this(null);
    }

    public LearningPage(String resourcePath) {
        this.resourcePath = resourcePath;
    }

    public String getResourcePath() {
        return resourcePath;
    }

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

    public String getShortDescription() {
        return shortDescription;
    }

    public void setShortDescription(String shortDescription) {
        this.shortDescription = shortDescription;
    }

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public List<LearningContentSection> getSections() {
        return sections;
    }

    public void setSections(List<LearningContentSection> sections) {
        this.sections = sections;
    }
}
