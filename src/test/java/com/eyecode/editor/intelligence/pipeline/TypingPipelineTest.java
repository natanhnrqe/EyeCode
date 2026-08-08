package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.intelligence.events.DocumentChangeListener;
import com.eyecode.editor.intelligence.events.DocumentTextChangeEvent;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class TypingPipelineTest {

    private static final EditorCommand INSERT_HELLO = new EditorCommand() {
        @Override
        public String name() {
            return "insert-hello";
        }

        @Override
        public void execute(EditorCommandContext context) {
            context.insertText(0, "hello");
            context.insertText(5, " world");
        }

        @Override
        public void undo(EditorCommandContext context) {
        }
    };

    private static final EditorCommand FAILING_EDIT = new EditorCommand() {
        @Override
        public String name() {
            return "failing-edit";
        }

        @Override
        public void execute(EditorCommandContext context) {
            context.insertText(0, "partial");
            throw new IllegalStateException("boom");
        }

        @Override
        public void undo(EditorCommandContext context) {
        }
    };

    private static SmartEditStrategy claiming(String key, EditorCommand command) {
        return new SmartEditStrategy() {
            @Override
            public SmartEditPriority priority() {
                return SmartEditPriority.HIGH;
            }

            @Override
            public boolean supports(EditorInputEvent event, EditorCommandContext context) {
                return event.isCharacterTyped() && event.key().equals(key);
            }

            @Override
            public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
                return Optional.of(command);
            }
        };
    }

    private static EditorCommandContext context(EditorBuffer buffer) {
        return new EditorCommandContext(buffer);
    }

    @Test
    void routeReturnsCommandFromFirstApplicableStrategy() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", INSERT_HELLO));
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);

        Optional<EditorCommand> command = pipeline.route(
                EditorInputEvent.characterTyped('(', 0, 0, Set.of()), context(buffer));

        assertTrue(command.isPresent());
        assertEquals("insert-hello", command.get().name());
    }

    @Test
    void processRunsCommandAndMarksHandled() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", INSERT_HELLO));
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);

        SmartEditResult result = pipeline.process(
                EditorInputEvent.characterTyped('(', 0, 0, Set.of()), context(buffer));

        assertTrue(result.isHandled());
        assertEquals("insert-hello", result.command().get().name());
        assertEquals("hello world", document.getText());
    }

    @Test
    void processCommitsAsSingleUndoableGroup() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", INSERT_HELLO));
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);

        pipeline.process(EditorInputEvent.characterTyped('(', 0, 0, Set.of()), context(buffer));

        buffer.undo();
        assertEquals("", document.getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void processFiresSingleMergedEvent() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", INSERT_HELLO));
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);
        List<DocumentTextChangeEvent> events = new ArrayList<>();
        document.addDocumentChangeListener((DocumentChangeListener) events::add);

        pipeline.process(EditorInputEvent.characterTyped('(', 0, 0, Set.of()), context(buffer));

        assertEquals(1, events.size());
        assertTrue(events.get(0).isTransactional());
        assertEquals("hello world", events.get(0).getAfter().getText());
    }

    @Test
    void processRollsBackOnFailureLeavingNoPartialState() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", FAILING_EDIT));
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument(null, "seed");
        EditorBuffer buffer = new EditorBuffer(document);

        SmartEditResult result = pipeline.process(
                EditorInputEvent.characterTyped('(', 3, 1, Set.of()), context(buffer));

        assertTrue(result.failed());
        assertEquals("boom", result.failure().get().getMessage());
        assertEquals("seed", document.getText());
        assertFalse(buffer.canUndo());
    }

    @Test
    void passthroughOnlyPipelineNeverHandlesCommonKey() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new PassthroughSmartEditStrategy());
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);

        SmartEditResult typed = pipeline.process(
                EditorInputEvent.characterTyped('a', 0, 0, Set.of()), context(buffer));
        SmartEditResult pressed = pipeline.process(
                EditorInputEvent.keyPressed("ENTER", 0, 0, Set.of()), context(buffer));

        assertFalse(typed.isHandled());
        assertFalse(pressed.isHandled());
        assertEquals("", document.getText());
    }

    @Test
    void processHandlesNullArguments() {
        TypingPipeline pipeline = new TypingPipeline(new SmartEditingRegistry());
        assertFalse(pipeline.process(null, context(new EditorBuffer(new EditorDocument()))).isHandled());
    }

    @Test
    void commandThatOnlyMovesCaretOpensNoTransaction() {
        SmartEditStrategy caretStrategy = new SmartEditStrategy() {
            @Override
            public SmartEditPriority priority() {
                return SmartEditPriority.NORMAL;
            }

            @Override
            public boolean supports(EditorInputEvent event, EditorCommandContext context) {
                return event.isKeyPressed() && event.key().equals("HOME");
            }

            @Override
            public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
                return Optional.of(new EditorCommand() {
                    @Override
                    public String name() {
                        return "move-home";
                    }

                    @Override
                    public void execute(EditorCommandContext ctx) {
                        ctx.moveCaret(ctx.snapshot().lineMap() != null
                                ? new com.eyecode.editor.v2.EditorPosition(0, 0)
                                : ctx.caret());
                    }

                    @Override
                    public void undo(EditorCommandContext ctx) {
                    }
                });
            }
        };

        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(caretStrategy);
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument(null, "abc");
        EditorBuffer buffer = new EditorBuffer(document);
        buffer.moveCaret(new com.eyecode.editor.v2.EditorPosition(0, 2));

        SmartEditResult result = pipeline.process(
                EditorInputEvent.keyPressed("HOME", 2, 1, Set.of()), context(buffer));

        assertTrue(result.isHandled());
        assertEquals("abc", document.getText());
        assertEquals(new com.eyecode.editor.v2.EditorPosition(0, 0), buffer.getCaret());
        assertFalse(buffer.canUndo());
    }

    @Test
    void snapshotReflectsCurrentVersionDuringExecution() {
        SmartEditStrategy snapshotStrategy = new SmartEditStrategy() {
            @Override
            public SmartEditPriority priority() {
                return SmartEditPriority.NORMAL;
            }

            @Override
            public boolean supports(EditorInputEvent event, EditorCommandContext context) {
                return event.isCharacterTyped();
            }

            @Override
            public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
                return Optional.of(new EditorCommand() {
                    @Override
                    public String name() {
                        return "record-version";
                    }

                    @Override
                    public void execute(EditorCommandContext ctx) {
                        DocumentSnapshot before = ctx.snapshot();
                        ctx.insertText(0, "x");
                        DocumentSnapshot after = ctx.snapshot();
                        assertTrue(after.version() > before.version());
                    }

                    @Override
                    public void undo(EditorCommandContext ctx) {
                    }
                });
            }
        };

        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(snapshotStrategy);
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);

        SmartEditResult result = pipeline.process(
                EditorInputEvent.characterTyped('a', 0, 0, Set.of()), context(buffer));

        assertTrue(result.isHandled());
        assertEquals("x", document.getText());
    }
}
