package com.eyecode.learning.model;

public record RelatedConcept(String id, String title) {

    public static RelatedConcept of(String id, String title) {
        return new RelatedConcept(id != null ? id : "", title != null ? title : "");
    }

    public static RelatedConcept fromTitle(String title) {
        return new RelatedConcept(title != null ? title : "", title != null ? title : "");
    }
}
