package com.eyecode.learning.content;

import java.util.List;

public record LearningMetadata(
        String id,
        String title,
        String concept,
        String level,
        int duration,
        String category,
        DocumentationTarget officialDocs,
        List<String> related,
        String next,
        String parent,
        List<LearningMember> members,
        LearningDepth depth,
        LearningKind kind,
        String sourceMember,
        String sourceSignature
) {

    public LearningMetadata(String id, String title, String concept, String level,
                            int duration, String category, DocumentationTarget officialDocs,
                            List<String> related, String next) {
        this(id, title, concept, level, duration, category, officialDocs, related, next,
                null, List.of(), LearningDepth.FULL);
    }

    public LearningMetadata(String id, String title, String concept, String level,
                            int duration, String category, DocumentationTarget officialDocs,
                            List<String> related, String next, LearningDepth depth) {
        this(id, title, concept, level, duration, category, officialDocs, related, next,
                null, List.of(), depth);
    }

    public LearningMetadata(String id, String title, String concept, String level,
                            int duration, String category, DocumentationTarget officialDocs,
                            List<String> related, String next, List<LearningMember> members,
                            LearningDepth depth) {
        this(id, title, concept, level, duration, category, officialDocs, related, next,
                null, members, depth);
    }

    public LearningMetadata(String id, String title, String concept, String level,
                            int duration, String category, DocumentationTarget officialDocs,
                            List<String> related, String next, String parent,
                            List<LearningMember> members, LearningDepth depth) {
        this(id, title, concept, level, duration, category, officialDocs, related, next,
                parent, members, depth, LearningKind.CONCEPT);
    }

    public LearningMetadata(String id, String title, String concept, String level,
                            int duration, String category, DocumentationTarget officialDocs,
                            List<String> related, String next, String parent,
                            List<LearningMember> members, LearningDepth depth,
                            LearningKind kind) {
        this(id, title, concept, level, duration, category, officialDocs, related, next,
                parent, members, depth, kind, null, null);
    }

    public LearningMetadata {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Learning metadata id must not be blank");
        }
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("Learning metadata title must not be blank");
        }
        if (concept == null || concept.isBlank()) {
            throw new IllegalArgumentException("Learning metadata concept must not be blank");
        }
        if (level == null || level.isBlank()) {
            throw new IllegalArgumentException("Learning metadata level must not be blank");
        }
        if (duration < 0) {
            throw new IllegalArgumentException("Learning metadata duration must not be negative");
        }
        if (category == null || category.isBlank()) {
            throw new IllegalArgumentException("Learning metadata category must not be blank");
        }
        depth = depth == null ? LearningDepth.FULL : depth;
        related = related == null ? List.of() : List.copyOf(related);
        parent = parent == null || parent.isBlank() ? null : parent.trim();
        members = members == null ? List.of() : List.copyOf(members);
        kind = kind == null ? LearningKind.CONCEPT : kind;
        sourceMember = sourceMember == null || sourceMember.isBlank() ? null : sourceMember.trim();
        sourceSignature = sourceSignature == null || sourceSignature.isBlank()
                ? null : sourceSignature.trim();
    }
}
