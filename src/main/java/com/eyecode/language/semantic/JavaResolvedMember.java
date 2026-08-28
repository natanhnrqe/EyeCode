package com.eyecode.language.semantic;

public record JavaResolvedMember(
        String name,
        JavaMemberKind kind,
        String owner,
        String returnType,
        String signature
) {
}
