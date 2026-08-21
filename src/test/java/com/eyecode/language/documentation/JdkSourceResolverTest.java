package com.eyecode.language.documentation;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JdkSourceResolverTest {

    private final JdkSourceResolver resolver = new JdkSourceResolver();

    @Test
    void resolvesSupportedJdkEntries() {
        assertEntry("java.lang.String", "java.base/java/lang/String.java", "String.java");
        assertEntry("java.lang.Object", "java.base/java/lang/Object.java", "Object.java");
        assertEntry("java.util.List", "java.base/java/util/List.java", "List.java");
        assertEntry("java.util.ArrayList", "java.base/java/util/ArrayList.java", "ArrayList.java");
        assertEntry("java.util.HashMap", "java.base/java/util/HashMap.java", "HashMap.java");
        assertEntry("javax.swing.UIManager", "java.desktop/javax/swing/UIManager.java", "UIManager.java");
    }

    @Test
    void unsupportedTypeHasNoCatalogIdentity() {
        assertTrue(JavaJdkTypeCatalog.findQualified("com.example.Widget").isEmpty());
    }

    private void assertEntry(String qualifiedName, String path, String displayName) {
        JavaJdkType type = JavaJdkTypeCatalog.findQualified(qualifiedName).orElseThrow();
        JdkSourceTarget target = resolver.resolve(type).orElseThrow();
        assertEquals(qualifiedName, target.qualifiedName());
        assertEquals(path, target.sourceEntryPath());
        assertEquals(displayName, target.displayName());
    }
}
