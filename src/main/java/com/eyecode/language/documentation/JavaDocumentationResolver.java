package com.eyecode.language.documentation;

import com.eyecode.language.semantic.DefinitionLocation;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.learning.content.DocumentationTarget;

import java.util.Optional;

public final class JavaDocumentationResolver {

    public static final String JAVA_VERSION = "21";
    private static final String API_ROOT =
            "https://docs.oracle.com/en/java/javase/" + JAVA_VERSION + "/docs/api/";

    public Optional<DocumentationTarget> resolve(Symbol symbol) {
        return resolveType(symbol).map(this::target);
    }

    public Optional<JavaJdkType> resolveType(Symbol symbol) {
        if (symbol == null || !isType(symbol)) {
            return Optional.empty();
        }
        return JavaJdkTypeCatalog.findQualified(symbol.qualifiedName());
    }

    public Optional<JavaJdkType> resolveType(String qualifiedName) {
        return JavaJdkTypeCatalog.findQualified(qualifiedName);
    }

    public Optional<DocumentationTarget> resolve(DefinitionLocation location) {
        return location == null ? Optional.empty() : resolve(location.symbol());
    }

    Optional<DocumentationTarget> resolve(JavaJdkType type) {
        String path = type.qualifiedName().replace('.', '/') + ".html";
        return Optional.of(new DocumentationTarget(type.simpleName(), API_ROOT + type.module() + "/" + path));
    }

    private static boolean isType(Symbol symbol) {
        return switch (symbol.kind()) {
            case TYPE, INTERFACE, ENUM, ANNOTATION -> true;
            default -> false;
        };
    }

    DocumentationTarget target(JavaJdkType type) {
        return resolve(type).orElseThrow();
    }
}
