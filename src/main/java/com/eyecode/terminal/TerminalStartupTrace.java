package com.eyecode.terminal;

import java.util.concurrent.atomic.AtomicLong;

public final class TerminalStartupTrace {
    private static final AtomicLong NEXT_ID = new AtomicLong();

    private final long id = NEXT_ID.incrementAndGet();
    private final long startedAt = System.nanoTime();

    public void mark(String stage) {
        long elapsedMillis = (System.nanoTime() - startedAt) / 1_000_000;
        System.out.printf("TERMINAL_STARTUP id=%d elapsedMs=%d stage=%s thread=%s%n",
                id, elapsedMillis, stage, Thread.currentThread().getName());
    }
}