package com.eyecode.language.documentation;

import com.eyecode.editor.intelligence.document.TextRange;
import com.eyecode.language.symbol.Symbol;
import com.eyecode.language.symbol.SymbolId;
import com.eyecode.language.symbol.SymbolKind;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class JavaDocumentationResolverTest {

    private final JavaDocumentationResolver resolver = new JavaDocumentationResolver();
    private final DocumentationAtCaretResolver atCaret = new DocumentationAtCaretResolver();

    @Test
    void resolvesGoldenJdkTypeToJava21ApiUrl() {
        var target = resolver.resolve(symbol(SymbolKind.TYPE, "String", "java.lang.String")).orElseThrow();

        assertEquals("String", target.label());
        assertEquals(
                "https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html",
                target.url());
    }

    @Test
    void resolvesImportAndImplicitJavaLangTypes() {
        String source = "import java.util.List; class Demo { List<String> values; String name; }";

        assertEquals("List", atCaret.resolve(source, source.indexOf("List<String>")).orElseThrow().label());
        assertEquals("String", atCaret.resolve(source, source.lastIndexOf("String name")).orElseThrow().label());
    }

    @Test
    void resolvesFullyQualifiedAndDesktopTypes() {
        String source = "class Demo { java.util.ArrayList<String> values; javax.swing.UIManager manager; }";

        assertEquals("ArrayList", atCaret.resolve(source, source.indexOf("ArrayList")).orElseThrow().label());
        assertEquals(
                "https://docs.oracle.com/en/java/javase/21/docs/api/java.desktop/javax/swing/UIManager.html",
                atCaret.resolve(source, source.indexOf("UIManager")).orElseThrow().url());
    }

    @Test
    void suppressesInvalidContextsAndProjectTypes() {
        assertTrue(atCaret.resolve("// String", 4).isEmpty());
        assertTrue(atCaret.resolve("String value = \"String\";", 16).isEmpty());
        assertTrue(atCaret.resolve("char value = 'x';", 15).isEmpty());
        assertTrue(atCaret.resolve("class String {}", 6).isEmpty());
        assertTrue(atCaret.resolve("class Demo { int value; }", 18).isEmpty());
        assertTrue(atCaret.resolve("class Demo { new Unknown(); }", 19).isEmpty());
    }

    @Test
    void rejectsNonTypeAndUnknownModules() {
        assertTrue(resolver.resolve(symbol(SymbolKind.METHOD, "String", "java.lang.String")).isEmpty());
        assertTrue(resolver.resolve(symbol(SymbolKind.TYPE, "Widget", "com.example.Widget")).isEmpty());
        assertTrue(JavaJdkTypeCatalog.findSimple("String").isPresent());
        assertTrue(JavaJdkTypeCatalog.findSimple("Widget").isEmpty());
    }

    private static Symbol symbol(SymbolKind kind, String name, String qualifiedName) {
        TextRange range = new TextRange(0, 1);
        return new Symbol(SymbolId.of(1L, range, kind), kind, name, range, 1L, 1L, qualifiedName);
    }
}
