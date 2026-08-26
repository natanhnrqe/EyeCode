package com.eyecode.javafx.learning;

import com.eyecode.learning.ui.LearningHoverScheduler;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.animation.PauseTransition;
import javafx.util.Duration;

public final class JavaFxLearningHoverScheduler implements LearningHoverScheduler {

    private Runnable hoverTask;
    private Runnable monitorTask;
    private final PauseTransition initialHoverTimer = new PauseTransition(Duration.millis(320));
    private final PauseTransition hoverTimer = new PauseTransition(Duration.millis(500));
    private final Timeline monitorTimer = new Timeline(
            new KeyFrame(Duration.millis(40), event -> {
                if (monitorTask != null) {
                    monitorTask.run();
                }
            }));

    public JavaFxLearningHoverScheduler() {
        initialHoverTimer.setOnFinished(event -> {
            if (hoverTask != null) {
                hoverTask.run();
            }
        });
        hoverTimer.setOnFinished(event -> {
            if (hoverTask != null) {
                hoverTask.run();
            }
        });
        monitorTimer.setCycleCount(Animation.INDEFINITE);
    }

    @Override
    public void restartHover(Runnable task) {
        hoverTask = task;
        hoverTimer.playFromStart();
    }

    @Override
    public void restartInitialHover(Runnable task) {
        hoverTask = task;
        initialHoverTimer.playFromStart();
    }

    @Override
    public void stopHover() {
        initialHoverTimer.stop();
        hoverTimer.stop();
        hoverTask = null;
    }

    @Override
    public void startMonitor(Runnable task) {
        monitorTask = task;
        if (monitorTimer.getStatus() != Animation.Status.RUNNING) {
            monitorTimer.playFromStart();
        }
    }

    @Override
    public void stopMonitor() {
        monitorTimer.stop();
        monitorTask = null;
    }

    @Override
    public void dispose() {
        stopHover();
        stopMonitor();
    }
}
