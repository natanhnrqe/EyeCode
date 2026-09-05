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
        add(types, "Number", "java.lang.Number", "java.base");
        add(types, "Byte", "java.lang.Byte", "java.base");
        add(types, "Short", "java.lang.Short", "java.base");
        add(types, "Long", "java.lang.Long", "java.base");
        add(types, "Float", "java.lang.Float", "java.base");
        add(types, "Double", "java.lang.Double", "java.base");
        add(types, "Boolean", "java.lang.Boolean", "java.base");
        add(types, "Character", "java.lang.Character", "java.base");
        add(types, "CharSequence", "java.lang.CharSequence", "java.base");
        add(types, "StringBuilder", "java.lang.StringBuilder", "java.base");
        add(types, "Comparable", "java.lang.Comparable", "java.base");
        add(types, "Iterable", "java.lang.Iterable", "java.base");
        add(types, "AutoCloseable", "java.lang.AutoCloseable", "java.base");
        add(types, "Throwable", "java.lang.Throwable", "java.base");
        add(types, "Exception", "java.lang.Exception", "java.base");
        add(types, "RuntimeException", "java.lang.RuntimeException", "java.base");
        add(types, "Error", "java.lang.Error", "java.base");
        add(types, "System", "java.lang.System", "java.base");
        add(types, "Math", "java.lang.Math", "java.base");
        add(types, "List", "java.util.List", "java.base");
        add(types, "ArrayList", "java.util.ArrayList", "java.base");
        add(types, "LinkedList", "java.util.LinkedList", "java.base");
        add(types, "Map", "java.util.Map", "java.base");
        add(types, "HashMap", "java.util.HashMap", "java.base");
        add(types, "Arrays", "java.util.Arrays", "java.base");
        add(types, "Collection", "java.util.Collection", "java.base");
        add(types, "Iterator", "java.util.Iterator", "java.base");
        add(types, "Collections", "java.util.Collections", "java.base");
        add(types, "Set", "java.util.Set", "java.base");
        add(types, "HashSet", "java.util.HashSet", "java.base");
        add(types, "LinkedHashSet", "java.util.LinkedHashSet", "java.base");
        add(types, "TreeSet", "java.util.TreeSet", "java.base");
        add(types, "LinkedHashMap", "java.util.LinkedHashMap", "java.base");
        add(types, "TreeMap", "java.util.TreeMap", "java.base");
        add(types, "Queue", "java.util.Queue", "java.base");
        add(types, "Deque", "java.util.Deque", "java.base");
        add(types, "ArrayDeque", "java.util.ArrayDeque", "java.base");
        add(types, "PriorityQueue", "java.util.PriorityQueue", "java.base");
        add(types, "Comparator", "java.util.Comparator", "java.base");
        add(types, "FunctionalInterface", "java.lang.FunctionalInterface", "java.base");
        add(types, "Runnable", "java.lang.Runnable", "java.base");
        add(types, "Supplier", "java.util.function.Supplier", "java.base");
        add(types, "Consumer", "java.util.function.Consumer", "java.base");
        add(types, "Function", "java.util.function.Function", "java.base");
        add(types, "Predicate", "java.util.function.Predicate", "java.base");
        add(types, "UnaryOperator", "java.util.function.UnaryOperator", "java.base");
        add(types, "BinaryOperator", "java.util.function.BinaryOperator", "java.base");
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
