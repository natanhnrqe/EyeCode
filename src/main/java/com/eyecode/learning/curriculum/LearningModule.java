package com.eyecode.learning.curriculum;

import com.eyecode.learning.model.DifficultyLevel;

import java.util.List;

public final class LearningModule {

    private String id;
    private String title;
    private String description;
    private DifficultyLevel difficulty;
    private List<LearningLesson> lessons;

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

    public DifficultyLevel getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(DifficultyLevel difficulty) {
        this.difficulty = difficulty;
    }

    public List<LearningLesson> getLessons() {
        return lessons;
    }

    public void setLessons(List<LearningLesson> lessons) {
        this.lessons = lessons;
    }
}
