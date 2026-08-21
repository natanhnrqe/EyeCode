package com.eyecode.language.documentation;

public record JdkSourceTarget(
        String qualifiedName,
        String module,
        String sourceEntryPath,
        String displayName
) {
    public JdkSourceTarget {
        if (qualifiedName == null || qualifiedName.isBlank()
                || module == null || module.isBlank()
                || sourceEntryPath == null || sourceEntryPath.isBlank()
                || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("JDK source target fields must not be blank");
        }
    }

    public String tabId() {
        return "jdk-source:" + qualifiedName;
    }
}
