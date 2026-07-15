package com.eyecode.learning.ui;

import javax.swing.Timer;

public final class SwingLearningHoverScheduler implements LearningHoverScheduler {

    private static final int HOVER_DELAY_MS = 500;
    private static final int MONITOR_INTERVAL_MS = 40;

    private final Timer hoverTimer;
    private final Timer monitorTimer;

    private Runnable hoverTask;
    private Runnable monitorTask;

    public SwingLearningHoverScheduler() {
        hoverTimer = new Timer(HOVER_DELAY_MS, event -> runHoverTask());
        hoverTimer.setRepeats(false);

        monitorTimer = new Timer(MONITOR_INTERVAL_MS, event -> runMonitorTask());
    }

    @Override
    public void restartHover(Runnable task) {
        hoverTask = task;
        hoverTimer.restart();
    }

    @Override
    public void stopHover() {
        hoverTimer.stop();
        hoverTask = null;
    }

    @Override
    public void startMonitor(Runnable task) {
        monitorTask = task;
        monitorTimer.start();
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

    private void runHoverTask() {
        if (hoverTask != null) {
            hoverTask.run();
        }
    }

    private void runMonitorTask() {
        if (monitorTask != null) {
            monitorTask.run();
        }
    }
}
