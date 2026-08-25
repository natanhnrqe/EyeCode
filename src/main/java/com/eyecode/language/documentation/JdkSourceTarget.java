package com.eyecode.language.documentation;

public record JdkSourceTarget(
        String qualifiedName,
        String module,
        String sourceEntryPath,
        String displayName,
        String memberName
) {
    public JdkSourceTarget(String qualifiedName, String module, String sourceEntryPath,
                           String displayName) {
        this(qualifiedName, module, sourceEntryPath, displayName, null);
    }

    public JdkSourceTarget {
        if (qualifiedName == null || qualifiedName.isBlank()
                || module == null || module.isBlank()
                || sourceEntryPath == null || sourceEntryPath.isBlank()
                || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("JDK source target fields must not be blank");
        }
        memberName = memberName == null || memberName.isBlank() ? null : memberName.trim();
    }

    public String tabId() {
        return "jdk-source:" + qualifiedName;
    }

    public String sourceIdentity() {
        return "jdk://" + module + "/" + sourceEntryPath;
    }

    public JdkSourceTarget withMember(String memberName) {
        return new JdkSourceTarget(qualifiedName, module, sourceEntryPath, displayName, memberName);
    }
}
