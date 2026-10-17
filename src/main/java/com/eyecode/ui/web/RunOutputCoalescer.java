package com.eyecode.ui.web;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Coalesces run output before it reaches the web shell bridge.
 *
 * <p>Programs that print in tight loops used to publish one bridge event per
 * read chunk, flooding the transport and freezing the interface. This buffer
 * merges chunks and publishes only on a short timer — or synchronously through
 * {@link #flush()} at run boundaries — so the event rate stays bounded no
 * matter how fast the program prints. A single flush publishes at most
 * {@link #MAX_FLUSH_CHARS} characters; when the program outprints the display,
 * the excess head is dropped and announced in the flush itself, keeping the
 * newest output visible while the run executes.</p>
 *
 * <p>State is per run: {@link #reset()} is invoked on every run boundary so a
 * later execution starts with empty buffers and a clean transport (a previous
 * execution can never leave the console mute).</p>
 */
final class RunOutputCoalescer implements AutoCloseable {

    interface Sink {
        void publish(String text, boolean error);
    }

    static final int MAX_FLUSH_CHARS = 128_000;
    private static final long FLUSH_PERIOD_MS = 100;

    private final Sink sink;
    private final Object lock = new Object();
    private final StringBuilder outputBuffer = new StringBuilder();
    private final StringBuilder errorBuffer = new StringBuilder();
    private final ScheduledExecutorService scheduler;

    RunOutputCoalescer(Sink sink) {
        this.sink = sink;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(task -> {
            Thread thread = new Thread(task, "eyecode-run-output-flush");
            thread.setDaemon(true);
            return thread;
        });
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                flush();
            } catch (RuntimeException ignored) {
                // the timer must survive a failed publish
            }
        }, FLUSH_PERIOD_MS, FLUSH_PERIOD_MS, TimeUnit.MILLISECONDS);
    }

    void onOutput(String text, boolean error) {
        if (text == null) {
            return;
        }
        synchronized (lock) {
            (error ? errorBuffer : outputBuffer).append(text);
        }
    }

    /**
     * Publishes everything still buffered, bounded by {@link #MAX_FLUSH_CHARS}
     * per channel. Deterministic and thread safe.
     */
    void flush() {
        synchronized (lock) {
            publishBuffer(false);
            publishBuffer(true);
        }
    }

    /** Discards pending text and restarts per-run state at a run boundary. */
    void reset() {
        synchronized (lock) {
            outputBuffer.setLength(0);
            errorBuffer.setLength(0);
        }
    }

    private void publishBuffer(boolean error) {
        StringBuilder buffer = error ? errorBuffer : outputBuffer;
        if (buffer.length() == 0) {
            return;
        }
        String text = buffer.toString();
        try {
            if (text.length() > MAX_FLUSH_CHARS) {
                int dropped = text.length() - MAX_FLUSH_CHARS;
                sink.publish("…[" + dropped + " caracteres descartados no transporte — saída mais rápida que a exibição]…\n", error);
                text = text.substring(dropped);
            }
            sink.publish(text, error);
            buffer.setLength(0);
        } catch (RuntimeException publishFailed) {
            // keep the text buffered: the next flush retries the publication
        }
    }

    @Override
    public void close() {
        flush();
        scheduler.shutdownNow();
    }
}
