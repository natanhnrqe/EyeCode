package com.eyecode.learning.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningContentRepositoryTest {

    private final LearningContentRepository repository = new LearningContentRepository();

    @Test
    void loadsBundledMarkdownByLogicalIdentifier() {
        String markdown = repository.load("java/basics/variables");

        assertTrue(markdown.startsWith("---"));
        assertTrue(markdown.contains("String playerName = \"Ada\""));
    }

    @Test
    void exposesStructuredMetadataAndBody() {
        LearningDocument document = repository.loadDocument("java/types/class");

        assertEquals("java/types/class", document.metadata().id());
        assertEquals("Classes em Java", document.metadata().title());
        assertEquals("class", document.metadata().concept());
        assertEquals("beginner", document.metadata().level());
        assertEquals(16, document.metadata().duration());
        assertEquals("JAVA CONCEPT", document.metadata().category());
        assertEquals("Java Classes and Objects", document.metadata().officialDocs().label());
        assertEquals(List.of("java/types/object", "java/types/interface", "java/types/record"),
                document.metadata().related());
        assertEquals("java/types/object", document.metadata().next());
        assertTrue(!document.markdownBody().startsWith("---"));
    }

    @Test
    void exposesStringMemberDestinationsFromFrontMatter() {
        LearningMetadata metadata = repository.loadDocument("java/jdk/string").metadata();

        assertEquals(6, metadata.members().size());
        assertEquals("length()", metadata.members().getFirst().label());
        assertEquals("java/jdk/string/length", metadata.members().getFirst().identifier());
    }

    @Test
    void exposesParentMetadataForMemberAndCollectionLessons() {
        assertEquals("java/jdk/string", repository.loadDocument("java/jdk/string/substring")
                .metadata().parent());
        assertEquals("java/jdk/list", repository.loadDocument("java/jdk/array-list")
                .metadata().parent());
        assertEquals("java/jdk/map", repository.loadDocument("java/jdk/hash-map")
                .metadata().parent());
        assertEquals(null, repository.loadDocument("java/jdk/string").metadata().parent());
        assertEquals(LearningKind.MEMBER,
                repository.loadDocument("java/jdk/string/contains").metadata().kind());
        assertEquals("contains",
                repository.loadDocument("java/jdk/string/contains").metadata().sourceMember());
    }

    @Test
    void producesDeterministicClasspathResourcePath() {
        assertEquals("/learning/content/java/basics/variables.md",
                repository.resourcePath("/java/basics/variables/"));
    }

    @Test
    void rejectsMissingOrUnsafeIdentifiers() {
        assertThrows(IllegalArgumentException.class, () -> repository.load(""));
        assertThrows(IllegalArgumentException.class, () -> repository.load("../variables"));
        assertThrows(IllegalArgumentException.class, () -> repository.load("java/basics/missing"));
    }

    @Test
    void classifiesInternalLearningLinksWithoutTreatingExternalLinksAsLessons() {
        assertEquals("java/types/object",
                LearningLink.identifier(LearningLink.toUri("java/types/object")).orElseThrow());
        assertTrue(LearningLink.identifier("https://docs.oracle.com/").isEmpty());
        assertTrue(LearningLink.identifier("eyecode://learn/../secret").isEmpty());
    }
}
