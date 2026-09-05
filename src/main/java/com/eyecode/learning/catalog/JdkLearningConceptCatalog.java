package com.eyecode.learning.catalog;

import com.eyecode.language.documentation.JavaJdkType;
import com.eyecode.language.documentation.JavaJdkTypeCatalog;
import com.eyecode.language.semantic.JavaMemberTarget;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.content.LearningContentRepository;
import com.eyecode.learning.model.ConceptType;
import com.eyecode.learning.model.DifficultyLevel;
import com.eyecode.learning.model.LearningConcept;

import java.util.Map;
import java.util.Optional;

/** Maps supported JDK types to concise, bundled learning lessons. */
public final class JdkLearningConceptCatalog {

    private static final Map<String, String> IDS = Map.ofEntries(
            Map.entry("String", "java/jdk/string"),
            Map.entry("Object", "java/jdk/object"),
            Map.entry("Integer", "java/jdk/integer"),
            Map.entry("Number", "java/jdk/number"),
            Map.entry("Byte", "java/jdk/byte"),
            Map.entry("Short", "java/jdk/short"),
            Map.entry("Long", "java/jdk/long"),
            Map.entry("Float", "java/jdk/float"),
            Map.entry("Double", "java/jdk/double"),
            Map.entry("Boolean", "java/jdk/boolean"),
            Map.entry("Character", "java/jdk/character"),
            Map.entry("CharSequence", "java/jdk/char-sequence"),
            Map.entry("StringBuilder", "java/jdk/string-builder"),
            Map.entry("Comparable", "java/jdk/comparable"),
            Map.entry("Iterable", "java/jdk/iterable"),
            Map.entry("AutoCloseable", "java/jdk/auto-closeable"),
            Map.entry("Throwable", "java/jdk/throwable"),
            Map.entry("Exception", "java/jdk/exception"),
            Map.entry("RuntimeException", "java/jdk/runtime-exception"),
            Map.entry("Error", "java/jdk/error"),
            Map.entry("System", "java/jdk/system"),
            Map.entry("Math", "java/jdk/math"),
            Map.entry("List", "java/jdk/list"),
            Map.entry("ArrayList", "java/jdk/array-list"),
            Map.entry("LinkedList", "java/jdk/linked-list"),
            Map.entry("Map", "java/jdk/map"),
            Map.entry("HashMap", "java/jdk/hash-map"));

    private final LearningContentRepository repository = new LearningContentRepository();
    private final Map<String, Map<String, String>> memberIndexes = new java.util.concurrent.ConcurrentHashMap<>();

    public Optional<LearningConcept> find(String name) {
        return JavaJdkTypeCatalog.findSimple(name).flatMap(this::create);
    }

    public Optional<LearningConcept> find(JavaMemberTarget target) {
        if (target == null || target.memberName() == null) {
            return Optional.empty();
        }
        return JavaJdkTypeCatalog.findQualified(target.ownerQualifiedName())
                .flatMap(type -> memberIdentifier(type, target.memberName())
                        .flatMap(identifier -> create(type, identifier)));
    }

    private Optional<LearningConcept> create(JavaJdkType type) {
        String id = IDS.get(type.simpleName());
        if (id == null) {
            return Optional.empty();
        }
        return create(type, id);
    }

    private Optional<LearningConcept> create(JavaJdkType type, String id) {
        if (id == null) {
            return Optional.empty();
        }
        LearningConcept concept = new LearningConcept();
        concept.setId(id);
        concept.setTitle(type.simpleName());
        concept.setDescription("A practical introduction to " + type.simpleName() + " in Java.");
        concept.setType(type.simpleName().equals("List") || type.simpleName().equals("Map")
                ? ConceptType.INTERFACE : ConceptType.CLASS);
        concept.setDifficulty(DifficultyLevel.BEGINNER);
        concept.setQualifiedName(type.qualifiedName());
        LearningPage page = new LearningPage("/learning/content/" + id + ".md");
        page.setId(id);
        concept.setPage(page);
        return Optional.of(concept);
    }

    private Optional<String> memberIdentifier(JavaJdkType type, String memberName) {
        String rootId = IDS.get(type.simpleName());
        if (rootId == null) {
            return Optional.empty();
        }
        try {
            Map<String, String> members = memberIndexes.computeIfAbsent(rootId, ignored ->
                    repository.loadDocument(rootId).metadata().members().stream()
                            .collect(java.util.stream.Collectors.toUnmodifiableMap(
                                    member -> member.label().replaceAll("\\(.*\\)$", ""),
                                    member -> member.identifier(),
                                    (first, ignoredDuplicate) -> first)));
            return Optional.ofNullable(members.get(memberName));
        } catch (RuntimeException ignored) {
            return Optional.empty();
        }
    }
}
