package com.eyecode.language.documentation;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

/** Small, read-only catalog for the first supported JDK documentation set. */
public final class JavaJdkTypeCatalog {

    private static final Map<String, JavaJdkType> BY_SIMPLE_NAME = createTypes().values().stream()
            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                    JavaJdkType::simpleName, type -> type));
    private static final Map<String, JavaJdkType> BY_QUALIFIED_NAME = createTypes();

    private JavaJdkTypeCatalog() {
    }

    public static Optional<JavaJdkType> findSimple(String name) {
        return Optional.ofNullable(BY_SIMPLE_NAME.get(name));
    }

    public static Optional<JavaJdkType> findQualified(String name) {
        return Optional.ofNullable(BY_QUALIFIED_NAME.get(name));
    }

    private static Map<String, JavaJdkType> createTypes() {
        Map<String, JavaJdkType> types = new LinkedHashMap<>();
        add(types, "String", "java.lang.String", "java.base");
        add(types, "Object", "java.lang.Object", "java.base");
        add(types, "Integer", "java.lang.Integer", "java.base");
        add(types, "System", "java.lang.System", "java.base");
        add(types, "Math", "java.lang.Math", "java.base");
        add(types, "List", "java.util.List", "java.base");
        add(types, "ArrayList", "java.util.ArrayList", "java.base");
        add(types, "Map", "java.util.Map", "java.base");
        add(types, "HashMap", "java.util.HashMap", "java.base");
        add(types, "UIManager", "javax.swing.UIManager", "java.desktop");
        return Map.copyOf(types);
    }

    private static void add(Map<String, JavaJdkType> types,
                            String simpleName,
                            String qualifiedName,
                            String module) {
        types.put(qualifiedName, new JavaJdkType(simpleName, qualifiedName, module));
    }
}
