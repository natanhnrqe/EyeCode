package com.eyecode.javafx.learning;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;

public final class MonacoLearningHoverPipeline {

    public enum Outcome {
        SHOW,
        SAME_TARGET,
        CANCELLED,
        NO_SEMANTIC_TARGET,
        STALE,
        LEFT_BEFORE_DELAY,
        ERROR
    }

    private final MonacoLearningIntentTimer timer;
    private final MonacoLearningTargetResolver resolver;
    private final MonacoLearningOverlayPresenter presenter;
    private final Consumer<Runnable> uiDispatcher;
    private Consumer<Outcome> outcomes = ignored -> { };

    private MonacoLearningTarget currentTarget;
    private MonacoLearningContent currentContent;
    private boolean intentReady;
    private boolean visible;
    private boolean editorTargetHovered;
    private boolean overlayHovered;
    private boolean disposed;
    private long generation;

    public MonacoLearningHoverPipeline(MonacoLearningIntentTimer timer,
                                       MonacoLearningTargetResolver resolver,
                                       MonacoLearningOverlayPresenter presenter,
                                       Consumer<Runnable> uiDispatcher) {
        this.timer = Objects.requireNonNull(timer, "timer");
        this.resolver = Objects.requireNonNull(resolver, "resolver");
        this.presenter = Objects.requireNonNull(presenter, "presenter");
        this.uiDispatcher = Objects.requireNonNull(uiDispatcher, "uiDispatcher");
    }

    public void setOutcomeListener(Consumer<Outcome> listener) {
        outcomes = listener == null ? ignored -> { } : listener;
    }

    public boolean isVisible() {
        return visible;
    }

    public void enterTarget(MonacoLearningTarget target) {
        if (disposed || target == null) return;
        editorTargetHovered = true;
        if (target.sameIdentity(currentTarget)) {
            outcomes.accept(Outcome.SAME_TARGET);
            return;
        }

        currentTarget = target;
        currentContent = null;
        intentReady = false;
        long requestGeneration = ++generation;
        timer.cancel();
        if (visible) timer.scheduleSwitch(() -> completeIntent(requestGeneration, target));
        else timer.scheduleInitial(() -> completeIntent(requestGeneration, target));

        try {
            resolver.resolve(target).whenComplete((content, failure) ->
                    uiDispatcher.accept(() -> completeResolution(requestGeneration, target, content, failure)));
        } catch (RuntimeException failure) {
            completeResolution(requestGeneration, target, Optional.empty(), failure);
        }
    }

    public void leaveEditorTarget() {
        if (disposed) return;
        editorTargetHovered = false;
        if (!overlayHovered) {
            cancelPending(Outcome.LEFT_BEFORE_DELAY);
        }
    }

    public void setOverlayHovered(boolean hovered) {
        if (disposed) return;
        overlayHovered = hovered;
    }

    public void onOverlayHidden() {
        if (disposed) return;
        visible = false;
        overlayHovered = false;
    }

    public void hardHide() {
        if (disposed) return;
        cancelPending(Outcome.CANCELLED);
        presenter.hardHide();
        visible = false;
        overlayHovered = false;
    }

    public void dispose() {
        if (disposed) return;
        hardHide();
        disposed = true;
        timer.dispose();
    }

    private void completeIntent(long requestGeneration, MonacoLearningTarget target) {
        if (!isCurrent(requestGeneration, target)) {
            outcomes.accept(Outcome.STALE);
            return;
        }
        intentReady = true;
        presentIfReady(requestGeneration, target);
    }

    private void completeResolution(long requestGeneration, MonacoLearningTarget target,
                                    Optional<MonacoLearningContent> content, Throwable failure) {
        if (!isCurrent(requestGeneration, target)) {
            outcomes.accept(Outcome.STALE);
            return;
        }
        if (failure != null) {
            outcomes.accept(Outcome.ERROR);
            return;
        }
        if (content == null || content.isEmpty()) {
            outcomes.accept(Outcome.NO_SEMANTIC_TARGET);
            return;
        }
        currentContent = content.get();
        presentIfReady(requestGeneration, target);
    }

    private void presentIfReady(long requestGeneration, MonacoLearningTarget target) {
        if (!intentReady || currentContent == null || !editorTargetHovered || !isCurrent(requestGeneration, target)) {
            return;
        }
        presenter.present(target, currentContent, visible);
        visible = true;
        outcomes.accept(Outcome.SHOW);
    }

    private void cancelPending(Outcome outcome) {
        generation++;
        currentTarget = null;
        currentContent = null;
        intentReady = false;
        timer.cancel();
        outcomes.accept(outcome);
    }

    private boolean isCurrent(long requestGeneration, MonacoLearningTarget target) {
        return !disposed && requestGeneration == generation && target.sameIdentity(currentTarget);
    }
}
