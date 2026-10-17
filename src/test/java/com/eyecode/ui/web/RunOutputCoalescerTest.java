package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunOutputCoalescerTest {

    private static final class RecordingSink implements RunOutputCoalescer.Sink {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<Boolean> errors = new CopyOnWriteArrayList<>();
        int failuresLeft;

        @Override
        public void publish(String text, boolean error) {
            if (failuresLeft > 0) {
                failuresLeft--;
                throw new IllegalStateException("publish failed");
            }
            events.add(text);
            errors.add(error);
        }

        String joined() {
            return String.join("", events);
        }
    }

    @Test
    void buffersSmallChunksUntilFlushKeepingOrderAndContent() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            for (int index = 0; index < 500; index++) {
                coalescer.onOutput("linha-" + index + "\n", false);
            }
            assertEquals(List.of(), sink.events, "nothing may be published before a flush");
            coalescer.flush();
        }
        assertTrue(sink.events.size() <= 2, "many small chunks must be coalesced: " + sink.events.size());
        StringBuilder expected = new StringBuilder();
        for (int index = 0; index < 500; index++) {
            expected.append("linha-").append(index).append('\n');
        }
        assertEquals(expected.toString(), sink.joined());
        assertTrue(sink.errors.stream().allMatch(error -> !error));
    }

    @Test
    void keepsStdoutAndStderrChannelsSeparate() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            coalescer.onOutput("fora\n", false);
            coalescer.onOutput("erro\n", true);
            coalescer.onOutput("mais\n", false);
            coalescer.flush();
        }
        assertEquals("fora\nmais\n", sink.events.get(0));
        assertEquals("erro\n", sink.events.get(1));
        assertEquals(false, sink.errors.get(0));
        assertEquals(true, sink.errors.get(1));
    }

    @Test
    void floodFlushDropsOnlyTheHeadAndAnnouncesIt() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            for (int index = 0; index < 10_000; index++) {
                coalescer.onOutput("aaaaaaaaaaaaaaaaaaaaaa\n", false);
            }
            coalescer.flush();
        }
        String joined = sink.joined();
        assertTrue(joined.contains("caracteres descartados no transporte"),
                "the dropped head must be announced honestly");
        String tail = sink.events.get(sink.events.size() - 1);
        assertTrue(tail.endsWith("aaaaaaaaaaaaaaaaaaaaaa\n"));
        assertTrue(joined.length() <= RunOutputCoalescer.MAX_FLUSH_CHARS + 200,
                "a flush publishes at most the tail cap: " + joined.length());
    }

    @Test
    void resetDiscardsPendingTextSoTheNextRunStartsClean() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            coalescer.onOutput("resto da execucao anterior\n", false);
            coalescer.reset();
            coalescer.flush();
            assertEquals(List.of(), sink.events, "pending text from a finished run must never leak");

            coalescer.onOutput("SEGUNDA_EXECUCAO\n", false);
            coalescer.flush();
        }
        assertEquals("SEGUNDA_EXECUCAO\n", sink.joined());
    }

    @Test
    void aFailingPublishDoesNotKillTheBuffer() {
        RecordingSink sink = new RecordingSink();
        sink.failuresLeft = 1;
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            coalescer.onOutput("primeiro\n", false);
            try {
                coalescer.flush();
            } catch (RuntimeException expectedFailure) {
                // a direct flush propagates the error to its caller
            }
            assertEquals(List.of(), sink.events, "the failed flush published nothing");
            coalescer.onOutput("segundo\n", false);
            coalescer.flush();
        }
        assertEquals("primeiro\nsegundo\n", sink.joined(),
                "the buffered text must survive a failed publish and reach the next flush");
    }

    @Test
    void emptyFlushPublishesNothing() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            coalescer.flush();
            coalescer.flush();
        }
        assertEquals(List.of(), sink.events);
    }
}
