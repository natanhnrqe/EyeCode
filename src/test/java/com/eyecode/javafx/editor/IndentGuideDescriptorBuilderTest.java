package com.eyecode.javafx.editor;

import javafx.application.Platform;
import org.fxmisc.richtext.CodeArea;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class IndentGuideDescriptorBuilderTest {

    private final IndentGuideDescriptorBuilder builder = new IndentGuideDescriptorBuilder();

    @BeforeAll
    static void startToolkit() {
        try {
            Platform.startup(() -> {});
        } catch (IllegalStateException ignored) {
        }
    }

    @Test
    void oneIndentedLineCreatesSingleParagraphDescriptor() throws Exception {
        assertEquals(List.of(new IndentGuideDescriptor(4, 0, 0)),
                buildFromText("    value++;"));
    }

    @Test
    void contiguousNestedLinesCreateSharedRangeDescriptors() throws Exception {
        assertEquals(List.of(new IndentGuideDescriptor(4, 1, 1)), buildFromText("""
                if (ok) {
                    run();
                }
                """));
    }

    @Test
    void blankLineWithoutIndentationBreaksTheRange() throws Exception {
        assertEquals(List.of(
                new IndentGuideDescriptor(4, 0, 0),
                new IndentGuideDescriptor(4, 2, 2)
        ), buildFromText("    foo();\n\n    bar();"));
    }

    @Test
    void blankLineWithOwnIndentationKeepsTheRange() throws Exception {
        assertEquals(List.of(new IndentGuideDescriptor(4, 0, 2)),
                buildFromText("    foo();\n    \n    bar();"));
    }

    @Test
    void tabsAndMixedIndentationUseLogicalColumns() throws Exception {
        assertEquals(List.of(new IndentGuideDescriptor(4, 0, 1)),
                buildFromText("\tvalue++;\n  \t  next();"));
    }

    private List<IndentGuideDescriptor> buildFromText(String text) throws Exception {
        CountDownLatch done = new CountDownLatch(1);
        AtomicReference<List<IndentGuideDescriptor>> result = new AtomicReference<>();
        AtomicReference<Throwable> failure = new AtomicReference<>();
        Platform.runLater(() -> {
            try {
                CodeArea area = new CodeArea();
                area.replaceText(text);
                result.set(builder.build(area.getParagraphs()));
            } catch (Throwable throwable) {
                failure.set(throwable);
            } finally {
                done.countDown();
            }
        });
        assertTrue(done.await(10, TimeUnit.SECONDS), "JavaFX task timed out");
        if (failure.get() != null) {
            throw new AssertionError(failure.get());
        }
        return result.get();
    }
}
