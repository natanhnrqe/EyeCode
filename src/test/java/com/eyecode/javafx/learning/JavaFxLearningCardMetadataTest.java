package com.eyecode.javafx.learning;

import com.eyecode.learning.content.DocumentationTarget;
import com.eyecode.learning.content.LearningMetadata;
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
