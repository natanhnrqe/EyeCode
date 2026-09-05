package com.eyecode.language.documentation;

public record JdkSourceTarget(
        String qualifiedName,
        String module,
        String sourceEntryPath,
        String displayName,
        String memberName,
        String memberSignature
) {
    public JdkSourceTarget(String qualifiedName, String module, String sourceEntryPath,
                           String displayName) {
        this(qualifiedName, module, sourceEntryPath, displayName, null, null);
    }

    public JdkSourceTarget(String qualifiedName, String module, String sourceEntryPath,
                           String displayName, String memberName) {
        this(qualifiedName, module, sourceEntryPath, displayName, memberName, null);
    }

    public JdkSourceTarget {
        if (qualifiedName == null || qualifiedName.isBlank()
                || module == null || module.isBlank()
                || sourceEntryPath == null || sourceEntryPath.isBlank()
                || displayName == null || displayName.isBlank()) {
            throw new IllegalArgumentException("JDK source target fields must not be blank");
        }
        memberName = memberName == null || memberName.isBlank() ? null : memberName.trim();
        memberSignature = memberSignature == null || memberSignature.isBlank()
                ? null : memberSignature.trim();
    }

    public String tabId() {
        return "jdk-source:" + qualifiedName;
    }

    public String sourceIdentity() {
        return "jdk://" + module + "/" + sourceEntryPath;
    }

    public JdkSourceTarget withMember(String memberName) {
        return new JdkSourceTarget(qualifiedName, module, sourceEntryPath, displayName,
                memberName, memberSignature);
    }

    public JdkSourceTarget withMemberSignature(String memberSignature) {
        return new JdkSourceTarget(qualifiedName, module, sourceEntryPath, displayName,
                memberName, memberSignature);
    }
}
