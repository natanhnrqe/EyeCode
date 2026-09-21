package com.eyecode.language.diagnostics;

import java.util.List;

public record DiagnosticEducation(String title, String summary, String explanation,
                                  String pattern, String correctedPattern, String tip,
                                  List<DiagnosticRelatedContent> relatedContent) {
    public DiagnosticEducation {
        title = title == null ? "" : title;
        summary = summary == null ? "" : summary;
        explanation = explanation == null ? "" : explanation;
        pattern = pattern == null ? "" : pattern;
        correctedPattern = correctedPattern == null ? "" : correctedPattern;
        tip = tip == null ? "" : tip;
        relatedContent = relatedContent == null ? List.of() : List.copyOf(relatedContent);
    }
}
