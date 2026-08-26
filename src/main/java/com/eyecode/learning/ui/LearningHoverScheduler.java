package com.eyecode.learning.ui;

public interface LearningHoverScheduler {

    void restartHover(Runnable task);

    default void restartInitialHover(Runnable task) {
        restartHover(task);
    }

    void stopHover();

    void startMonitor(Runnable task);

    void stopMonitor();

    void dispose();
}
