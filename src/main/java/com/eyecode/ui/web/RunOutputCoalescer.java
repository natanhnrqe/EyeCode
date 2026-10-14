package com.eyecode.ui.web;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coalesces run output before it reaches the web shell bridge.
 *
 * <p>Programs that print in tight loops used to publish one bridge event per
 * read chunk, flooding the transport and freezing the interface. This buffer
 * merges chunks and flushes when {@link #FLUSH_THRESHOLD_CHARS} accumulate, on
 * a short timer for liveness, or synchronously through {@link #flush()} when
 * the run finishes. The total published volume is capped: beyond the cap one
 * truncation marker is published and the remaining text is dropped, while the
 * underlying streams keep being drained by the run session.</p>
 */
final class RunOutputCoalescer implements AutoCloseable {

    interface Sink {
        void publish(String text, boolean error);
    }

    static final long DEFAULT_MAX_PUBLISHED_CHARS = 1_000_000;
    private static final int FLUSH_THRESHOLD_CHARS = 4096;
    private static final long FLUSH_PERIOD_MS = 100;

    private final Sink sink;
    private final long maxPublishedChars;
    private final Object lock = new Object();
    private final StringBuilder outputBuffer = new StringBuilder();
    private final StringBuilder errorBuffer = new StringBuilder();
    private final AtomicLong publishedChars = new AtomicLong();
    private final ScheduledExecutorService scheduler;
    private boolean truncated;

    RunOutputCoalescer(Sink sink) {
        this(sink, DEFAULT_MAX_PUBLISHED_CHARS);
    }

    RunOutputCoalescer(Sink sink, long maxPublishedChars) {
        this.sink = sink;
        this.maxPublishedChars = Math.max(FLUSH_THRESHOLD_CHARS, maxPublishedChars);
        this.scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "eyecode-run-output-flush");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler.scheduleAtFixedRate(this::flush, FLUSH_PERIOD_MS, FLUSH_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    void onOutput(String text, boolean error) {
        if (text == null) {
            return;
        }
        synchronized (lock) {
            if (truncated) {
                return;
            }
            (error ? errorBuffer : outputBuffer).append(text);
            if (outputBuffer.length() >= FLUSH_THRESHOLD_CHARS) {
                publishBuffer(false);
            }
            if (errorBuffer.length() >= FLUSH_THRESHOLD_CHARS) {
                publishBuffer(true);
            }
        }
    }

    /** Publishes everything still buffered. Deterministic and thread safe. */
    void flush() {
        synchronized (lock) {
            publishBuffer(false);
            publishBuffer(true);
        }
    }

    boolean isTruncated() {
        synchronized (lock) {
            return truncated;
        }
    }

    private void publishBuffer(boolean error) {
        StringBuilder buffer = error ? errorBuffer : outputBuffer;
        if (buffer.length() == 0) {
            return;
        }
        long remaining = maxPublishedChars - publishedChars.get();
        String text = buffer.toString();
        buffer.setLength(0);
        if (remaining <= 0) {
            publishTruncationMarker();
            return;
        }
        if (text.length() > remaining) {
            sink.publish(text.substring(0, (int) remaining), error);
            publishedChars.addAndGet(remaining);
            publishTruncationMarker();
            return;
        }
        sink.publish(text, error);
        publishedChars.addAndGet(text.length());
    }

    private void publishTruncationMarker() {
        if (truncated) {
            return;
        }
        truncated = true;
        sink.publish("\n…saída truncada: o limite de " + (maxPublishedChars / 1000) + " KB de saída foi atingido…\n", false);
    }

    @Override
    public void close() {
        flush();
        scheduler.shutdownNow();
    }
}
