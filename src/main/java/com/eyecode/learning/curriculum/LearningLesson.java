package com.eyecode.learning.curriculum;

import java.util.List;

public final class LearningLesson {

    private String id;
    private String title;
    private String description;
    private int order;
    private List<LearningConceptRef> concepts;

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

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public List<LearningConceptRef> getConcepts() {
        return concepts;
    }

    public void setConcepts(List<LearningConceptRef> concepts) {
        this.concepts = concepts;
    }
}
