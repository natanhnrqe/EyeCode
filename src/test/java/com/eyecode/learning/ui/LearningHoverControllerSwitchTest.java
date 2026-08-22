package com.eyecode.learning.ui;

import com.eyecode.editor.v2.syntax.SyntaxSnapshot;
import com.eyecode.editor.v2.syntax.SyntaxToken;
import com.eyecode.editor.v2.syntax.TokenType;
import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.hover.HoverEngine;
import com.eyecode.learning.model.LearningConcept;
import com.eyecode.learning.renderer.LearningCardRenderer;
import org.junit.jupiter.api.Test;

import java.awt.Point;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.IntConsumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LearningHoverControllerSwitchTest {

    @Test
    void briefTargetCrossingDoesNotReplaceVisibleCard() {
        Fixture fixture = new Fixture();
        fixture.move(0);
        fixture.scheduler.fireHover();
        fixture.move(2);
        fixture.move(0);
        fixture.scheduler.fireHover();

        assertEquals(List.of("String"), fixture.popup.shownTitles());
        assertEquals(List.of(), fixture.popup.updatedTitles());
    }

    @Test
    void stableTargetCrossingUpdatesTheSameCardAfterDelay() {
        Fixture fixture = new Fixture();
        fixture.move(0);
        fixture.scheduler.fireHover();
        fixture.move(2);

        assertEquals(List.of(), fixture.popup.updatedTitles());
        fixture.scheduler.fireHover();

        assertEquals(List.of("Object"), fixture.popup.updatedTitles());
        assertEquals(1, fixture.popup.externalRepositionCount);
        assertTrue(fixture.popup.visible);
    }

    @Test
    void repeatedSameTargetDoesNotReloadOrReposition() {
        Fixture fixture = new Fixture();
        fixture.move(0);
        fixture.scheduler.fireHover();
        fixture.move(0);
        fixture.move(0);
        fixture.scheduler.fireHover();

        assertEquals(List.of(), fixture.popup.updatedTitles());
        assertEquals(0, fixture.popup.externalRepositionCount);
    }

    @Test
    void enteringCardCancelsPendingTargetReplacement() {
        Fixture fixture = new Fixture();
        fixture.move(0);
        fixture.scheduler.fireHover();
        fixture.move(2);
        fixture.surface.insideEditor = false;
        fixture.popup.containsPointer = true;
        fixture.scheduler.fireMonitor();
        fixture.scheduler.fireHover();

        assertEquals(List.of(), fixture.popup.updatedTitles());
        assertTrue(fixture.popup.visible);
    }

    private static final class Fixture {
        private final FakeSurface surface = new FakeSurface();
        private final FakePopup popup = new FakePopup();
        private final FakeScheduler scheduler = new FakeScheduler();
        private final AtomicLong clock = new AtomicLong();
        private final LearningHoverController controller;

        private Fixture() {
            LearningConcept string = concept("String", "java/jdk/string");
            LearningConcept object = concept("Object", "java/jdk/object");
            SyntaxSnapshot syntax = new SyntaxSnapshot(List.of(
                    new SyntaxToken(TokenType.IDENTIFIER, 0, 1, "String"),
                    new SyntaxToken(TokenType.IDENTIFIER, 2, 3, "Object")));
            HoverEngine unusedKeywordEngine = context -> Optional.empty();
            controller = new LearningHoverController(
                    surface,
                    popup,
                    scheduler,
                    unusedKeywordEngine,
                    () -> syntax,
                    identifier -> "",
                    false,
                    offset -> Optional.of(offset < 2 ? string : object),
                    token -> Optional.empty(),
                    new HoverStateMachine(clock::get));
        }

        private void move(int offset) {
            surface.move(offset);
            clock.addAndGet(500L);
        }
    }

    private static LearningConcept concept(String title, String identifier) {
        LearningConcept concept = new LearningConcept();
        concept.setTitle(title);
        LearningPage page = new LearningPage(identifier);
        page.setId(identifier);
        concept.setPage(page);
        return concept;
    }

    private static final class FakeSurface implements LearningHoverSurface {
        private final List<IntConsumer> moveListeners = new ArrayList<>();
        private boolean insideEditor = true;

        @Override
        public void addMoveListener(IntConsumer listener) {
            moveListeners.add(listener);
        }

        @Override
        public void removeMoveListener(IntConsumer listener) {
            moveListeners.remove(listener);
        }

        @Override
        public void addCancelListener(Runnable listener) {
        }

        @Override
        public void removeCancelListener(Runnable listener) {
        }

        @Override
        public boolean containsScreen(Point point) {
            return insideEditor;
        }

        @Override
        public Point pointerScreenLocation() {
            return new Point(1, 1);
        }

        @Override
        public void dispose() {
        }

        private void move(int offset) {
            moveListeners.forEach(listener -> listener.accept(offset));
        }
    }

    private static final class FakePopup implements LearningCardRenderer {
        private final List<String> shown = new ArrayList<>();
        private final List<String> updated = new ArrayList<>();
        private int externalRepositionCount;
        private boolean visible;
        private boolean containsPointer;

        @Override
        public void show(LearningConcept concept) {
            visible = true;
            shown.add(concept.getTitle());
        }

        @Override
        public void hide() {
            visible = false;
        }

        @Override
        public boolean isVisible() {
            return visible;
        }

        @Override
        public void update(LearningConcept concept) {
            updated.add(concept.getTitle());
        }

        @Override
        public void updateForExternalHover(LearningConcept concept) {
            update(concept);
            externalRepositionCount++;
        }

        @Override
        public void loadHtml(String html) {
        }

        @Override
        public boolean containsScreen(Point screenPoint) {
            return containsPointer;
        }

        @Override
        public void dispose() {
        }

        private List<String> shownTitles() {
            return shown;
        }

        private List<String> updatedTitles() {
            return updated;
        }
    }

    private static final class FakeScheduler implements LearningHoverScheduler {
        private Runnable hoverTask;
        private Runnable monitorTask;

        @Override
        public void restartHover(Runnable task) {
            hoverTask = task;
        }

        @Override
        public void stopHover() {
            hoverTask = null;
        }

        @Override
        public void startMonitor(Runnable task) {
            monitorTask = task;
        }

        @Override
        public void stopMonitor() {
            monitorTask = null;
        }

        @Override
        public void dispose() {
            stopHover();
            stopMonitor();
        }

        private void fireHover() {
            Runnable task = hoverTask;
            hoverTask = null;
            if (task != null) {
                task.run();
            }
        }

        private void fireMonitor() {
            if (monitorTask != null) {
                monitorTask.run();
            }
        }
    }
}
