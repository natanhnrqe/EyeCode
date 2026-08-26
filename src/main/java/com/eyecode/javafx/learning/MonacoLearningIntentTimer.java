package com.eyecode.javafx.learning;

public interface MonacoLearningIntentTimer {
    void scheduleInitial(Runnable task);

    void scheduleSwitch(Runnable task);

    void cancel();

    default void dispose() {
        cancel();
    }
}
