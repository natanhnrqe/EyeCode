package com.eyecode.language.semantic;

import java.util.List;

public record JavaMemberTarget(
        String ownerQualifiedName,
        String memberName,
        JavaMemberKind memberKind,
        Integer argumentCount,
        List<String> signatureHints
) {
    public JavaMemberTarget {
        if (ownerQualifiedName == null || ownerQualifiedName.isBlank()
                || memberName == null || memberName.isBlank()
                || memberKind == null) {
            throw new IllegalArgumentException("Java member target fields must not be blank");
        }
        if (argumentCount != null && argumentCount < 0) {
            throw new IllegalArgumentException("Java member argument count must not be negative");
        }
        signatureHints = signatureHints == null ? List.of() : List.copyOf(signatureHints);
    }

    public JavaMemberTarget(String ownerQualifiedName, String memberName,
                            JavaMemberKind memberKind, Integer argumentCount) {
        this(ownerQualifiedName, memberName, memberKind, argumentCount, List.of());
    }

    public JavaMemberTarget(String ownerQualifiedName, String memberName,
                            JavaMemberKind memberKind) {
        this(ownerQualifiedName, memberName, memberKind, null, List.of());
    }
}
