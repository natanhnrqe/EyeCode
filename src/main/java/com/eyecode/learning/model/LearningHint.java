package com.eyecode.learning.model;

public final class LearningHint {

    private String title;
    private String description;
    private HintSeverity severity;
    private LearningConcept concept;

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

    public HintSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(HintSeverity severity) {
        this.severity = severity;
    }

    public LearningConcept getConcept() {
        return concept;
    }

    public void setConcept(LearningConcept concept) {
        this.concept = concept;
    }
}
