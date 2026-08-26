package com.eyecode.javafx.learning;

import com.eyecode.learning.content.LearningPage;
import com.eyecode.learning.model.LearningConcept;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MonacoLearningHoverPipelineTest {

    @Test
    void repeatedMovesInsideOneTargetStartOneResolution() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);

        fixture.pipeline.enterTarget(string);
        fixture.pipeline.enterTarget(string);
        fixture.pipeline.enterTarget(string);

        assertEquals(1, fixture.resolver.requests.get());
    }

    @Test
    void targetChangeDuringWaitingUsesTheLatestIntentWindow() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);
        MonacoLearningTarget object = target("Object", 8, 14);

        fixture.pipeline.enterTarget(string);
        fixture.pipeline.enterTarget(object);
        fixture.resolver.complete(string, content("String"));
        fixture.timer.fire();

        assertTrue(fixture.presenter.presented.isEmpty());

        fixture.resolver.complete(object, content("Object"));
        fixture.timer.fire();

        assertEquals(List.of("Object"), fixture.presenter.presented);
    }

    @Test
    void contentReadyBeforeIntentWaitsForIntent() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);

        fixture.pipeline.enterTarget(string);
        fixture.resolver.complete(string, content("String"));

        assertTrue(fixture.presenter.presented.isEmpty());
        fixture.timer.fire();
        assertEquals(List.of("String"), fixture.presenter.presented);
    }

    @Test
    void intentBeforeContentWaitsForContent() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);

        fixture.pipeline.enterTarget(string);
        fixture.timer.fire();
        assertTrue(fixture.presenter.presented.isEmpty());

        fixture.resolver.complete(string, content("String"));
        assertEquals(List.of("String"), fixture.presenter.presented);
    }

    @Test
    void staleResolutionCannotPresentAfterMovingToAnotherTarget() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);
        MonacoLearningTarget object = target("Object", 8, 14);

        fixture.pipeline.enterTarget(string);
        fixture.pipeline.enterTarget(object);
        fixture.timer.fire();
        fixture.resolver.complete(string, content("String"));
        fixture.resolver.complete(object, content("Object"));

        assertEquals(List.of("Object"), fixture.presenter.presented);
    }

    @Test
    void editorToOverlayDoesNotHideTheCard() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);

        fixture.pipeline.enterTarget(string);
        fixture.resolver.complete(string, content("String"));
        fixture.timer.fire();
        fixture.pipeline.leaveEditorTarget();
        fixture.pipeline.setOverlayHovered(true);

        assertFalse(fixture.presenter.hardHideRequested);
        assertEquals(List.of("String"), fixture.presenter.presented);
    }

    @Test
    void editorToBlankBecomesHiddenOnlyAfterDomGraceExpires() {
        Fixture fixture = new Fixture();
        fixture.pipeline.enterTarget(target("String", 0, 6));
        fixture.resolver.complete(target("String", 0, 6), content("String"));
        fixture.timer.fire();

        fixture.pipeline.leaveEditorTarget();
        fixture.pipeline.onOverlayHidden();

        assertFalse(fixture.pipeline.isVisible());
    }

    @Test
    void cardToEditorThenBlankRemainsVisibleUntilTheDomSignalsRemoval() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);
        fixture.pipeline.enterTarget(string);
        fixture.resolver.complete(string, content("String"));
        fixture.timer.fire();

        fixture.pipeline.leaveEditorTarget();
        fixture.pipeline.setOverlayHovered(true);
        fixture.pipeline.setOverlayHovered(false);

        assertTrue(fixture.pipeline.isVisible());
        fixture.pipeline.onOverlayHidden();
        assertFalse(fixture.pipeline.isVisible());
    }

    @Test
    void rapidTargetChangesPresentOnlyTheLatestTarget() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);
        MonacoLearningTarget object = target("Object", 8, 14);
        MonacoLearningTarget math = target("Math", 16, 20);

        fixture.pipeline.enterTarget(string);
        fixture.pipeline.enterTarget(object);
        fixture.pipeline.enterTarget(math);
        fixture.timer.fire();
        fixture.resolver.complete(string, content("String"));
        fixture.resolver.complete(object, content("Object"));
        fixture.resolver.complete(math, content("Math"));

        assertEquals(List.of("Math"), fixture.presenter.presented);
    }

    @Test
    void hardHideCancelsPendingResolution() {
        Fixture fixture = new Fixture();
        MonacoLearningTarget string = target("String", 0, 6);

        fixture.pipeline.enterTarget(string);
        fixture.pipeline.hardHide();
        fixture.timer.fire();
        fixture.resolver.complete(string, content("String"));

        assertTrue(fixture.presenter.presented.isEmpty());
        assertTrue(fixture.presenter.hardHideRequested);
    }

    private static MonacoLearningTarget target(String text, int start, int end) {
        return new MonacoLearningTarget("file:///Demo.java", 1, start, end, 1, start + 1, text);
    }

    private static MonacoLearningContent content(String title) {
        LearningConcept concept = new LearningConcept();
        concept.setTitle(title);
        LearningPage page = new LearningPage("java/jdk/" + title.toLowerCase());
        page.setId("java/jdk/" + title.toLowerCase());
        concept.setPage(page);
        return new MonacoLearningContent(concept, "<p>" + title + "</p>");
    }

    private static final class Fixture {
        private final FakeTimer timer = new FakeTimer();
        private final FakeResolver resolver = new FakeResolver();
        private final FakePresenter presenter = new FakePresenter();
        private final MonacoLearningHoverPipeline pipeline = new MonacoLearningHoverPipeline(
                timer, resolver, presenter, Runnable::run);
    }

    private static final class FakeTimer implements MonacoLearningIntentTimer {
        private Runnable task;

        @Override public void scheduleInitial(Runnable task) { this.task = task; }
        @Override public void scheduleSwitch(Runnable task) { this.task = task; }
        @Override public void cancel() { task = null; }

        private void fire() {
            Runnable current = task;
            task = null;
            if (current != null) current.run();
        }
    }

    private static final class FakeResolver implements MonacoLearningTargetResolver {
        private final AtomicInteger requests = new AtomicInteger();
        private final List<Entry> entries = new ArrayList<>();

        @Override public CompletableFuture<Optional<MonacoLearningContent>> resolve(MonacoLearningTarget target) {
            requests.incrementAndGet();
            CompletableFuture<Optional<MonacoLearningContent>> future = new CompletableFuture<>();
            entries.add(new Entry(target, future));
            return future;
        }

        private void complete(MonacoLearningTarget target, MonacoLearningContent content) {
            entries.stream().filter(entry -> entry.target.equals(target)).findFirst().orElseThrow()
                    .future.complete(Optional.of(content));
        }

        private record Entry(MonacoLearningTarget target,
                             CompletableFuture<Optional<MonacoLearningContent>> future) { }
    }

    private static final class FakePresenter implements MonacoLearningOverlayPresenter {
        private final List<String> presented = new ArrayList<>();
        private boolean softHideRequested;
        private boolean hardHideRequested;

        @Override public void present(MonacoLearningTarget target, MonacoLearningContent content, boolean replaceVisible) {
            presented.add(content.concept().getTitle());
        }

        @Override public void hardHide() { hardHideRequested = true; }
    }
}
