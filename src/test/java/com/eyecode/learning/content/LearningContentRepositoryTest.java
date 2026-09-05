package com.eyecode.learning.content;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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
    void parsesStructuredMetadataAndBodyFromControlledResource() {
        LearningFrontMatterParser.Parsed parsed = new LearningFrontMatterParser().parse("""
                ---
                id: test/structured
                title: Structured lesson
                concept: structured
                level: beginner
                duration: 7
                category: TEST CATEGORY
                officialDocs:
                  label: Test documentation
                  url: https://example.com/docs
                related:
                  - test/first
                  - test/second
                next: test/next
                ---
                Body content.
                """, "test/structured");
        LearningMetadata metadata = parsed.metadata();

        assertEquals("test/structured", metadata.id());
        assertEquals("Structured lesson", metadata.title());
        assertEquals("structured", metadata.concept());
        assertEquals("beginner", metadata.level());
        assertEquals(7, metadata.duration());
        assertEquals("TEST CATEGORY", metadata.category());
        assertEquals("Test documentation", metadata.officialDocs().label());
        assertEquals(List.of("test/first", "test/second"),
                metadata.related());
        assertEquals("test/next", metadata.next());
        assertEquals("Body content.", parsed.body());
    }

    @Test
    void loadsStableMetadataFromBundledClassLesson() {
        LearningDocument document = repository.loadDocument("java/types/class");

        assertEquals("java/types/class", document.metadata().id());
        assertEquals("Classes em Java", document.metadata().title());
        assertEquals("class", document.metadata().concept());
        assertEquals("CONCEITO JAVA", document.metadata().category());
        assertEquals("Java Classes and Objects", document.metadata().officialDocs().label());
        assertFalse(document.metadata().related().isEmpty());
        assertFalse(document.markdownBody().isBlank());
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
