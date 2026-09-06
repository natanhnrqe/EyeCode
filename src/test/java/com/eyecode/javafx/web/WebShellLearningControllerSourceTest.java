package com.eyecode.javafx.web;

import com.eyecode.language.documentation.JdkSourceResolver;
import com.eyecode.language.documentation.JdkSourceTarget;
import com.eyecode.learning.content.LearningContentEngine;
import com.eyecode.learning.content.LearningDocument;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WebShellLearningControllerSourceTest {

    private final LearningContentEngine contentEngine = new LearningContentEngine();
    private final JdkSourceResolver sourceResolver = new JdkSourceResolver();

    @Test
    void buildsSourceTargetsForCommonMethodCardsThroughWebShellPath() {
        Map<String, String> cards = Map.of(
                "java/jdk/string/is-blank", "isBlank",
                "java/jdk/string/char-at", "charAt",
                "java/jdk/map/compute-if-absent", "computeIfAbsent",
                "java/jdk/arrays/as-list", "asList",
                "java/jdk/optional/map", "map",
                "java/jdk/stream/filter", "filter",
                "java/jdk/comparator/comparing", "comparing");

        for (Map.Entry<String, String> card : cards.entrySet()) {
            LearningDocument document = contentEngine.loadDocument(card.getKey());
            LearningDocument parent = contentEngine.loadDocument(document.metadata().parent());

            JdkSourceTarget target = WebShellLearningController.sourceTarget(
                    document.metadata(), parent.metadata(), sourceResolver);

            assertNotNull(target, card.getKey());
            assertEquals(card.getValue(), target.memberName(), card.getKey());
        }
    }

    @Test
    void preservesExactAndFallbackSourceContracts() {
        LearningDocument exact = contentEngine.loadDocument("java/jdk/string/char-at");
        LearningDocument fallback = contentEngine.loadDocument("java/jdk/string/formatted");
        LearningDocument parent = contentEngine.loadDocument("java/jdk/string");

        JdkSourceTarget exactTarget = WebShellLearningController.sourceTarget(
                exact.metadata(), parent.metadata(), sourceResolver);
        JdkSourceTarget fallbackTarget = WebShellLearningController.sourceTarget(
                fallback.metadata(), parent.metadata(), sourceResolver);

        assertEquals("(int)", exactTarget.memberSignature());
        assertEquals(null, fallbackTarget.memberSignature());
        assertEquals("formatted", fallbackTarget.memberName());
    }

    @Test
    void auditsEveryJdkMethodCardThroughTargetConstruction() throws IOException {
        List<String> exact = new ArrayList<>();
        List<String> fallback = new ArrayList<>();
        List<String> unavailable = new ArrayList<>();
        List<String> intentional = new ArrayList<>();

        try (var paths = Files.walk(Path.of("src/main/resources/learning/content/java/jdk"))) {
            paths.filter(path -> path.toString().endsWith(".md"))
                    .filter(this::isMethodCard)
                    .forEach(path -> classify(path, exact, fallback, unavailable, intentional));
        }

        assertEquals(65, exact.size());
        assertEquals(139, fallback.size());
        assertEquals(List.of(), unavailable);
        assertEquals(List.of(), intentional);
        assertEquals(204, exact.size() + fallback.size());
    }

    private boolean isMethodCard(Path path) {
        try {
            return Files.readString(path).contains("\nkind: method\n");
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private void classify(Path path, List<String> exact, List<String> fallback,
                          List<String> unavailable, List<String> intentional) {
        try {
            String source = Files.readString(path);
            String identifier = source.lines()
                    .filter(line -> line.startsWith("id: "))
                    .findFirst()
                    .orElseThrow()
                    .substring(4);
            LearningDocument document = contentEngine.loadDocument(identifier);
            String parentId = document.metadata().parent();
            if (parentId == null) {
                unavailable.add(identifier);
                return;
            }
            LearningDocument parent = contentEngine.loadDocument(parentId);
            JdkSourceTarget target = WebShellLearningController.sourceTarget(
                    document.metadata(), parent.metadata(), sourceResolver);
            if (target == null) {
                unavailable.add(identifier);
            } else if (target.memberName() == null) {
                intentional.add(identifier);
            } else if (target.memberSignature() == null) {
                fallback.add(identifier);
            } else {
                exact.add(identifier);
            }
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
