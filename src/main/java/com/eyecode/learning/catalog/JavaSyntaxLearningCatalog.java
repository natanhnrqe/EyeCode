package com.eyecode.learning.catalog;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.LinkedHashMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Deterministic exact-token index for common Java syntax lessons. */
public final class JavaSyntaxLearningCatalog {

    private final Map<String, LearningConcept> concepts;

    public JavaSyntaxLearningCatalog() {
        Map<String, LearningConcept> entries = new LinkedHashMap<>();
        register(entries, "class", "java/types/class", "Class", "type", List.of("new", "extends", "implements"));
        register(entries, "interface", "java/types/interface", "Interface", "type", List.of("implements", "class"));
        register(entries, "enum", "java/types/enum", "Enum", "type", List.of("class"));
        register(entries, "record", "java/types/record", "Record", "type", List.of("class"));

        register(entries, "public", "java/syntax/visibility/public", "public", "visibility", List.of("private", "protected"));
        register(entries, "private", "java/syntax/visibility/private", "private", "visibility", List.of("public", "protected"));
        register(entries, "protected", "java/syntax/visibility/protected", "protected", "visibility", List.of("public", "private"));
        register(entries, "static", "java/syntax/modifiers/static", "static", "modifier", List.of("final", "class"));
        register(entries, "final", "java/syntax/modifiers/final", "final", "modifier", List.of("static", "class"));
        register(entries, "abstract", "java/syntax/modifiers/abstract", "abstract", "modifier", List.of("interface", "class"));

        register(entries, "new", "java/syntax/objects/new", "new", "object", List.of("java/types/class"));
        register(entries, "this", "java/syntax/objects/this", "this", "object", List.of("super", "constructor"));
        register(entries, "super", "java/syntax/objects/super", "super", "object", List.of("this", "extends"));

        register(entries, "if", "java/syntax/control-flow/if", "if", "control-flow", List.of("else", "switch"));
        register(entries, "else", "java/syntax/control-flow/else", "else", "control-flow", List.of("if"));
        register(entries, "switch", "java/syntax/control-flow/switch", "switch", "control-flow", List.of("case", "default"));
        register(entries, "case", "java/syntax/control-flow/case", "case", "control-flow", List.of("switch", "default"));
        register(entries, "default", "java/syntax/control-flow/default", "default", "control-flow", List.of("switch", "case"));
        register(entries, "for", "java/syntax/control-flow/for", "for", "control-flow", List.of("while", "break", "continue"));
        register(entries, "while", "java/syntax/control-flow/while", "while", "control-flow", List.of("for", "break"));
        register(entries, "do", "java/syntax/control-flow/do", "do-while", "control-flow", List.of("while"));
        register(entries, "break", "java/syntax/control-flow/break", "break", "control-flow", List.of("continue", "for"));
        register(entries, "continue", "java/syntax/control-flow/continue", "continue", "control-flow", List.of("break", "for"));
        register(entries, "return", "java/syntax/control-flow/return", "return", "control-flow", List.of("java/syntax/types/void"));

        register(entries, "try", "java/syntax/exceptions/try", "try", "exceptions", List.of("catch", "finally"));
        register(entries, "catch", "java/syntax/exceptions/catch", "catch", "exceptions", List.of("try", "finally"));
        register(entries, "finally", "java/syntax/exceptions/finally", "finally", "exceptions", List.of("try", "catch"));
        register(entries, "throw", "java/syntax/exceptions/throw", "throw", "exceptions", List.of("throws", "catch"));
        register(entries, "throws", "java/syntax/exceptions/throws", "throws", "exceptions", List.of("throw", "try"));

        register(entries, "extends", "java/syntax/types/extends", "extends", "type-system", List.of("implements", "super"));
        register(entries, "implements", "java/syntax/types/implements", "implements", "type-system", List.of("extends", "interface"));
        register(entries, "instanceof", "java/syntax/types/instanceof", "instanceof", "type-system", List.of("class", "null"));

        register(entries, "package", "java/syntax/organization/package", "package", "organization", List.of("import"));
        register(entries, "import", "java/syntax/organization/import", "import", "organization", List.of("package"));
        register(entries, "void", "java/syntax/types/void", "void", "type-system", List.of("return", "method"));
        register(entries, "var", "java/syntax/types/var", "var", "type-system", List.of("final", "class"));
        register(entries, "yield", "java/syntax/control-flow/yield", "yield", "control-flow", List.of("switch", "case", "default", "return"));
        register(entries, "byte", "java/basics/primitive-types", "primitive types", "type-system", List.of("int", "long", "boolean"));
        register(entries, "short", "java/basics/primitive-types", "primitive types", "type-system", List.of("int", "long", "boolean"));
        register(entries, "int", "java/basics/primitive-types", "primitive types", "type-system", List.of("byte", "long", "boolean"));
        register(entries, "long", "java/basics/primitive-types", "primitive types", "type-system", List.of("int", "double", "boolean"));
        register(entries, "float", "java/basics/primitive-types", "primitive types", "type-system", List.of("double", "int", "boolean"));
        register(entries, "double", "java/basics/primitive-types", "primitive types", "type-system", List.of("float", "long", "boolean"));
        register(entries, "char", "java/basics/primitive-types", "primitive types", "type-system", List.of("int", "boolean", "String"));
        register(entries, "boolean", "java/basics/primitive-types", "primitive types", "type-system", List.of("char", "int", "true"));
        register(entries, "null", "java/syntax/literals/null", "null", "literals", List.of("instanceof", "if"));
        register(entries, "true", "java/syntax/literals/true", "true", "literals", List.of("false", "if"));
        register(entries, "false", "java/syntax/literals/false", "false", "literals", List.of("true", "if"));
        concepts = Collections.unmodifiableMap(new LinkedHashMap<>(entries));
    }

    public Optional<LearningConcept> find(String token) {
        return Optional.ofNullable(concepts.get(token));
    }

    public List<LearningConcept> allConcepts() {
        return List.copyOf(concepts.values());
    }

    private static void register(Map<String, LearningConcept> entries, String trigger,
                                 String id, String title, String category, List<String> related) {
        LearningConcept concept = new LearningConcept();
        concept.setId(id);
        concept.setTitle(title);
        concept.setDescription("A practical Java syntax reference for " + title + ".");
        concept.setType(ConceptType.CLASS);
        concept.setDifficulty(DifficultyLevel.BEGINNER);
        concept.setTrigger(trigger);
        concept.setRelatedConcepts(List.copyOf(related));
        LearningPage page = new LearningPage("/learning/content/" + id + ".md");
        page.setId(id);
        concept.setPage(page);
        entries.put(trigger, concept);
    }
}
