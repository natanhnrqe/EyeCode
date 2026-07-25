package com.eyecode.learning.diagnostic;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class LifecycleLogger {

    private static final long START_NANOS = System.nanoTime();
    private static final AtomicReference<StackTraceElement[]> SHOW_STACK = new AtomicReference<>();
    private static final AtomicReference<StackTraceElement[]> HIDE_STACK = new AtomicReference<>();
    private static final AtomicReference<StackTraceElement[]> DISPOSE_STACK = new AtomicReference<>();
    private static final AtomicReference<StackTraceElement[]> LOAD_CONTENT_STACK = new AtomicReference<>();

    private LifecycleLogger() {
    }

    public static void show(Object owner, Object detail) {
        record("show()", owner, detail, SHOW_STACK);
    }

    public static void hide(Object owner) {
        record("hide()", owner, null, HIDE_STACK);
    }

    public static void dispose(Object owner) {
        record("dispose()", owner, null, DISPOSE_STACK);
    }

    public static void loadContent(Object owner, String html) {
        String detail = html == null ? "null" : "length=" + html.length();
        record("loadContent()", owner, detail, LOAD_CONTENT_STACK);
    }

    public static void workerState(Object owner, String state) {
        record("Worker.state", owner, state, null);
        if ("CANCELLED".equals(state)) {
            cancelled(owner);
        }
    }

    private static void record(String event, Object owner, Object detail, AtomicReference<StackTraceElement[]> stackRef) {
        stackRef.set(Thread.currentThread().getStackTrace());
        System.out.println("[LifecycleLogger] " + event + " renderer=" + owner.getClass().getSimpleName()
                + " time=" + elapsed() + " thread=" + Thread.currentThread().getName()
                + " detail=" + detail);
    }

    private static void cancelled(Object owner) {
        System.err.println("[LifecycleLogger] CANCELLED renderer=" + owner.getClass().getSimpleName()
                + " time=" + elapsed() + " thread=" + Thread.currentThread().getName());
        System.err.println("[LifecycleLogger] cancellation stack:");
        printStack("<cancellation> stack:", Thread.currentThread().getStackTrace());
        printStack("last show() stack:", SHOW_STACK.get());
        printStack("last hide() stack:", HIDE_STACK.get());
        printStack("last dispose() stack:", DISPOSE_STACK.get());
        printStack("last loadContent() stack:", LOAD_CONTENT_STACK.get());
    }

    private static void printStack(String label, StackTraceElement[] stack) {
        System.err.println("[LifecycleLogger] " + label);
        if (stack == null) {
            System.err.println("[LifecycleLogger] <not recorded>");
            return;
        }
        for (StackTraceElement element : stack) {
            System.err.println("[LifecycleLogger]   " + element);
        }
    }

    private static String elapsed() {
        long elapsedNanos = System.nanoTime() - START_NANOS;
        if (elapsedNanos < 0) elapsedNanos = 0;
        return String.format("%4dms", TimeUnit.NANOSECONDS.toMillis(elapsedNanos));
    }
}
