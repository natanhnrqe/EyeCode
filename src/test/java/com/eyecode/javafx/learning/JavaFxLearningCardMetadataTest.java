package com.eyecode.javafx.learning;

import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningMetadata;
import com.eyecode.learning.content.LearningMember;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.language.documentation.JdkSourceTarget;
import javafx.application.Platform;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaFxLearningCardMetadataTest {

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> { });
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void headerAndFooterProjectStructuredMetadata() throws Exception {
        runInFx(() -> {
            LearningMetadata metadata = new LearningMetadata(
                    "java/types/class",
                    "Classes em Java",
                    "class",
                    "beginner",
                    16,
                    "JAVA CONCEPT",
                    new DocumentationTarget("Java Classes", "https://docs.oracle.com/"),
                    List.of("java/types/object", "java/types/interface"),
                    "java/types/object"
            );
            JavaFxLearningCardHeader header = new JavaFxLearningCardHeader();
            JavaFxLearningCardFooter footer = new JavaFxLearningCardFooter();

            header.show(metadata);
            footer.show(metadata, ignored -> { }, ignored -> { }, ignored -> { }, id -> id);

            assertEquals("Classes em Java", header.titleForTest());
            assertTrue(header.subtitleForTest().contains("JAVA CONCEPT"));
            assertTrue(header.subtitleForTest().contains("BEGINNER"));
            assertTrue(header.subtitleForTest().contains("16 MIN"));
            assertEquals(3, footer.relatedCountForTest());
            assertEquals("Java Classes ↗", footer.documentationTextForTest());

            for (String concept : List.of("class", "interface", "enum", "record", "object")) {
                header.show(new LearningMetadata(
                        "java/types/" + concept, concept, concept, "beginner", 1,
                        "JAVA CONCEPT", null, List.of(), null));
                assertTrue(header.iconLoadedForTest(), concept);
            }
        });
    }

    @Test
    void sourceActionUsesCapabilityEvenWhenLessonMetadataHasNoSourceProse() throws Exception {
        runInFx(() -> {
            LearningMetadata metadata = new LearningMetadata(
                    "java/types/string", "String", "type", "intermediate", 1,
                    "JAVA TYPE", null, List.of(), null);
            JavaFxLearningCardFooter footer = new JavaFxLearningCardFooter();
            var opened = new java.util.concurrent.atomic.AtomicReference<JdkSourceTarget>();
            JdkSourceTarget target = new JdkSourceTarget(
                    "java.lang.String", "java.base",
                    "java.base/java/lang/String.java", "String.java");

            footer.show(metadata, ignored -> { }, ignored -> { }, ignored -> { },
                    id -> id, target, opened::set);

            assertTrue(footer.sourceVisibleForTest());
            footer.fireSourceForTest();
            assertEquals(target, opened.get());
        });
    }

    @Test
    void memberLinksUseTheExistingInternalNavigationCallback() throws Exception {
        runInFx(() -> {
            LearningMetadata metadata = new LearningMetadata(
                    "java/jdk/string", "String", "string", "beginner", 4,
                    "JAVA API", null, List.of(), null,
                    List.of(new LearningMember("substring()", "java/jdk/string/substring")),
                    com.eyecode.learning.content.LearningDepth.FULL);
            JavaFxLearningCardFooter footer = new JavaFxLearningCardFooter();
            var navigated = new java.util.concurrent.atomic.AtomicReference<String>();

            footer.show(metadata, ignored -> { }, ignored -> { }, ignored -> { }, id -> id,
                    null, ignored -> { }, navigated::set);

            assertEquals(2, footer.memberCountForTest());
            footer.fireMemberForTest(0);
            assertEquals("java/jdk/string/substring", navigated.get());
        });
    }

    @Test
    void breadcrumbIsNativeHeaderNavigationAndRootHasNoRedundantPath() throws Exception {
        runInFx(() -> {
            LearningMetadata root = new LearningMetadata(
                    "java/jdk/string", "String", "string", "beginner", 4,
                    "JAVA API", null, List.of(), null);
            LearningMetadata child = new LearningMetadata(
                    "java/jdk/string/substring", "String.substring()", "string-substring",
                    "beginner", 2, "JAVA API", null, List.of(), null,
                    "java/jdk/string", List.of(), com.eyecode.learning.content.LearningDepth.QUICK);
            JavaFxLearningCardHeader header = new JavaFxLearningCardHeader();
            var navigated = new java.util.concurrent.atomic.AtomicReference<String>();

            header.show(root);
            assertEquals("", header.breadcrumbTextForTest());

            header.show(child, List.of(root), navigated::set);
            assertEquals("String > substring()", header.breadcrumbTextForTest());
            header.fireBreadcrumbForTest(0);
            assertEquals("java/jdk/string", navigated.get());
        });
    }

    @Test
    void methodOwnerIsNotDuplicatedInRelatedFooter() throws Exception {
        runInFx(() -> {
            LearningMetadata metadata = new LearningMetadata(
                    "java/jdk/string/contains", "String.contains()", "string-contains",
                    "beginner", 1, "JAVA API", null, List.of("java/jdk/string"), null,
                    "java/jdk/string", List.of(), com.eyecode.learning.content.LearningDepth.QUICK,
                    com.eyecode.learning.content.LearningKind.MEMBER);
            JavaFxLearningCardFooter footer = new JavaFxLearningCardFooter();

            footer.show(metadata, ignored -> { }, ignored -> { }, ignored -> { }, id -> "String",
                    new DocumentationTarget("String API", "https://docs.oracle.com/"), null,
                    ignored -> { }, ignored -> { });

            assertEquals(0, footer.relatedLinkCountForTest());
            assertEquals("String API ↗", footer.documentationTextForTest());
        });
    }

    @Test
    void semanticAndInternalMemberCardsPreserveTheSameSourceTarget() throws Exception {
        runInFx(() -> {
            var opened = new java.util.ArrayList<JdkSourceTarget>();
            JavaFxLearningWorkspace workspace = new JavaFxLearningWorkspace(
                    ignored -> { }, opened::add);
            try {
                LearningConcept member = concept("java/jdk/string/contains", "java.lang.String");
                workspace.rendererForTest().show(member);
                workspace.rendererForTest().footerForTest().fireSourceForTest();

                workspace.rendererForTest().navigateToIdentifier("java/jdk/string");
                workspace.rendererForTest().footerForTest().fireMemberForTest(3);
                workspace.rendererForTest().footerForTest().fireSourceForTest();

                JdkSourceTarget expected = new JdkSourceTarget(
                        "java.lang.String", "java.base",
                        "java.base/java/lang/String.java", "String.java", "contains");
                assertEquals(List.of(expected, expected.withMemberSignature("(CharSequence)")), opened);

                workspace.rendererForTest().navigateToIdentifier("java/jdk/string");
                workspace.rendererForTest().footerForTest().fireSourceForTest();
                assertEquals(null, opened.getLast().memberName());
            } finally {
                workspace.dispose();
            }
        });
    }

    private static LearningConcept concept(String identifier, String qualifiedName) {
        LearningConcept concept = new LearningConcept();
        concept.setQualifiedName(qualifiedName);
        LearningPage page = new LearningPage(identifier);
        page.setId(identifier);
        concept.setPage(page);
        return concept;
    }

    private static void runInFx(ThrowingRunnable action) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                action.run();
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS));
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
