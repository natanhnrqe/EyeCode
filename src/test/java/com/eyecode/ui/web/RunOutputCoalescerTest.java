package com.eyecode.ui.web;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RunOutputCoalescerTest {

    private static final class RecordingSink implements RunOutputCoalescer.Sink {
        final List<String> events = new CopyOnWriteArrayList<>();
        final List<Boolean> errors = new CopyOnWriteArrayList<>();

        @Override
        public void publish(String text, boolean error) {
            events.add(text);
            errors.add(error);
        }

        String joined() {
            return String.join("", events);
        }
    }

    @Test
    void mergesChunksKeepingOrderAndContent() {
        RecordingSink sink = new RecordingSink();
        StringBuilder expected = new StringBuilder();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            for (int index = 0; index < 500; index++) {
                String line = "linha-" + index + "\n";
                expected.append(line);
                coalescer.onOutput(line, false);
            }
            coalescer.flush();
        }
        assertTrue(sink.events.size() <= 3, "a flood of small chunks must not become a flood of events: "
                + sink.events.size());
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
    void capsTotalVolumeAndMarksTruncation() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink, 4096)) {
            for (int index = 0; index < 2000; index++) {
                coalescer.onOutput("aaaaaaaaaa\n", false);
            }
            coalescer.flush();
        }
        assertTrue(sink.joined().contains("saída truncada"), "the cap must be announced honestly");
        assertTrue(sink.joined().length() <= 4096 + 200,
                "published volume must be bounded: " + sink.joined().length());
    }

    @Test
    void truncatedCoalescerDropsFurtherOutputWithoutNewEvents() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink, 4096)) {
            for (int index = 0; index < 2000; index++) {
                coalescer.onOutput("aaaaaaaaaa\n", false);
            }
            coalescer.flush();
            int afterCap = sink.events.size();
            for (int index = 0; index < 100; index++) {
                coalescer.onOutput("bbbbbbbbbb\n", false);
            }
            coalescer.flush();
            assertEquals(afterCap, sink.events.size());
        }
    }

    @Test
    void emptyFlushPublishesNothing() {
        RecordingSink sink = new RecordingSink();
        try (RunOutputCoalescer coalescer = new RunOutputCoalescer(sink)) {
            coalescer.flush();
            coalescer.flush();
        }
        assertEquals(List.of(), new ArrayList<>(sink.events));
    }
}
