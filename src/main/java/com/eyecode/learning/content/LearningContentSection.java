package com.eyecode.learning.content;

public final class LearningContentSection {

    private String id;
    private String title;
    private String content;
    private int order;
    private LearningContentType type;

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

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }

    public LearningContentType getType() {
        return type;
    }

    public void setType(LearningContentType type) {
        this.type = type;
    }
}
