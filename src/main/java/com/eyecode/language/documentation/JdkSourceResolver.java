package com.eyecode.language.documentation;

import java.util.Optional;

public final class JdkSourceResolver {

    public Optional<JdkSourceTarget> resolve(JavaJdkType type) {
        if (type == null) {
            return Optional.empty();
        }
        String path = type.qualifiedName().replace('.', '/') + ".java";
        return Optional.of(new JdkSourceTarget(
                type.qualifiedName(), type.module(), type.module() + "/" + path,
                type.simpleName() + ".java"));
    }
}
