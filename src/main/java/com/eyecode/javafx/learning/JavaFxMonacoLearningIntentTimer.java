package com.eyecode.javafx.learning;

import javafx.animation.PauseTransition;
import javafx.util.Duration;

public final class JavaFxMonacoLearningIntentTimer implements MonacoLearningIntentTimer {
    private static final Duration INITIAL_DELAY = Duration.millis(320);
    private static final Duration SWITCH_DELAY = Duration.millis(500);

    private final PauseTransition timer = new PauseTransition();

    @Override
    public void scheduleInitial(Runnable task) {
        schedule(INITIAL_DELAY, task);
    }

    @Override
    public void scheduleSwitch(Runnable task) {
        schedule(SWITCH_DELAY, task);
    }

    @Override
    public void cancel() {
        timer.stop();
    }

    private void schedule(Duration duration, Runnable task) {
        timer.stop();
        timer.setDuration(duration);
        timer.setOnFinished(event -> task.run());
        timer.playFromStart();
    }
}
