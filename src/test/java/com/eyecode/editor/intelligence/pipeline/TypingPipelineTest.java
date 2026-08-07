package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.intelligence.document.DocumentSnapshot;
import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.editor.v2.command.CommandManager;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class TypingPipelineTest {

    private static final EditorCommand INSERT_XYZ = new EditorCommand() {
        @Override
        public String name() {
            return "insert-xyz";
        }

        @Override
        public void execute(EditorCommandContext context) {
            context.document().insert(0, "xyz");
        }

        @Override
        public void undo(EditorCommandContext context) {
            context.document().delete(0, 3);
        }
    };

    private static EditorCommandContext context(EditorBuffer buffer) {
        EditorDocument document = buffer.getDocument();
        return new EditorCommandContext() {
            @Override
            public EditorBuffer buffer() {
                return buffer;
            }

            @Override
            public EditorDocument document() {
                return document;
            }

            @Override
            public DocumentSnapshot snapshot() {
                return document.snapshot();
            }

            @Override
            public CommandManager commandManager() {
                return buffer.getCommandManager();
            }
        };
    }

    private static SmartEditHandler handler(int priority, Optional<EditorCommand> result) {
        return new SmartEditHandler() {
            @Override
            public int priority() {
                return priority;
            }

            @Override
            public Optional<EditorCommand> tryHandle(EditorInputEvent event, EditorCommandContext context) {
                return result;
            }
        };
    }

    @Test
    void routeReturnsEmptyWhenNoHandlerClaims() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        TypingPipeline pipeline = new TypingPipeline(registry);

        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);

        Optional<EditorCommand> command = pipeline.route(
                EditorInputEvent.text(0, "("), context(buffer));

        assertTrue(command.isEmpty());
    }

    @Test
    void firstClaimingHandlerWinsByPriorityOrder() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(handler(20, Optional.of(INSERT_XYZ)));
        registry.register(handler(10, Optional.empty()));
        TypingPipeline pipeline = new TypingPipeline(registry);

        Optional<EditorCommand> command = pipeline.route(
                EditorInputEvent.text(0, "("), context(new EditorBuffer(new EditorDocument())));

        assertTrue(command.isPresent());
        assertEquals("insert-xyz", command.get().name());
    }

    @Test
    void higherPriorityHandlerConsultsLowerPriorityFirst() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(handler(10, Optional.of(INSERT_XYZ)));
        registry.register(handler(20, Optional.of(new NoOpCommand())));
        TypingPipeline pipeline = new TypingPipeline(registry);

        Optional<EditorCommand> command = pipeline.route(
                EditorInputEvent.text(0, "("), context(new EditorBuffer(new EditorDocument())));

        assertTrue(command.isPresent());
        assertEquals("insert-xyz", command.get().name());
    }

    @Test
    void unregisterStopsConsultation() {
        SmartEditHandler claiming = handler(0, Optional.of(INSERT_XYZ));
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming);
        TypingPipeline pipeline = new TypingPipeline(registry);

        assertTrue(pipeline.route(EditorInputEvent.text(0, "("),
                context(new EditorBuffer(new EditorDocument()))).isPresent());

        registry.unregister(claiming);

        assertTrue(pipeline.route(EditorInputEvent.text(0, "("),
                context(new EditorBuffer(new EditorDocument()))).isEmpty());
    }

    @Test
    void clearRemovesAllHandlers() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(handler(0, Optional.of(INSERT_XYZ)));
        registry.clear();
        assertEquals(0, registry.handlers().size());
    }

    @Test
    void commandExecutesAndUndoesThroughContext() {
        EditorDocument document = new EditorDocument();
        EditorBuffer buffer = new EditorBuffer(document);
        EditorCommandContext ctx = context(buffer);

        assertTrue(INSERT_XYZ.canExecute(ctx));
        INSERT_XYZ.execute(ctx);
        assertEquals("xyz", document.getText());
        INSERT_XYZ.undo(ctx);
        assertEquals("", document.getText());
    }

    @Test
    void registryRejectsNullHandler() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(null);
        assertTrue(registry.handlers().isEmpty());
    }

    private static final class NoOpCommand implements EditorCommand {
        @Override
        public String name() {
            return "no-op";
        }

        @Override
        public void execute(EditorCommandContext context) {
        }

        @Override
        public void undo(EditorCommandContext context) {
        }
    }
}
