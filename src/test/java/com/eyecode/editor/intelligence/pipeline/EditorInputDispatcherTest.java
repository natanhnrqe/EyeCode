package com.eyecode.editor.intelligence.pipeline;

import com.eyecode.editor.v2.EditorBuffer;
import com.eyecode.editor.v2.EditorDocument;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class EditorInputDispatcherTest {

    private static EditorCommand command(String name) {
        return new EditorCommand() {
            @Override
            public String name() {
                return name;
            }

            @Override
            public void execute(EditorCommandContext context) {
            }

            @Override
            public void undo(EditorCommandContext context) {
            }
        };
    }

    private static SmartEditStrategy claiming(String key, SmartEditPriority priority) {
        return new SmartEditStrategy() {
            @Override
            public SmartEditPriority priority() {
                return priority;
            }

            @Override
            public boolean supports(EditorInputEvent event, EditorCommandContext context) {
                return event.isCharacterTyped() && event.key().equals(key);
            }

            @Override
            public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
                return Optional.of(command("cmd-" + key));
            }
        };
    }

    private static EditorCommandContext context() {
        return new EditorCommandContext(new EditorBuffer(new EditorDocument()));
    }

    @Test
    void dispatchReturnsCommandFromFirstApplicableStrategy() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", SmartEditPriority.NORMAL));
        EditorInputDispatcher dispatcher = new EditorInputDispatcher(registry);

        Optional<EditorCommand> command = dispatcher.dispatch(
                EditorInputEvent.characterTyped('(', 0, 0, java.util.Set.of()), context());

        assertTrue(command.isPresent());
        assertEquals("cmd-(", command.get().name());
    }

    @Test
    void dispatchConsultsHigherPriorityFirst() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", SmartEditPriority.LOW));
        registry.register(claiming("(", SmartEditPriority.HIGH));
        EditorInputDispatcher dispatcher = new EditorInputDispatcher(registry);

        Optional<EditorCommand> command = dispatcher.dispatch(
                EditorInputEvent.characterTyped('(', 0, 0, java.util.Set.of()), context());

        assertTrue(command.isPresent());
        assertEquals("cmd-(", command.get().name());
    }

    @Test
    void dispatchContinuesWhenClaimingStrategyProducesNoCommand() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new SmartEditStrategy() {
            @Override
            public SmartEditPriority priority() {
                return SmartEditPriority.HIGH;
            }

            @Override
            public boolean supports(EditorInputEvent event, EditorCommandContext context) {
                return true;
            }

            @Override
            public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
                return Optional.empty();
            }
        });
        registry.register(claiming("(", SmartEditPriority.LOW));
        EditorInputDispatcher dispatcher = new EditorInputDispatcher(registry);

        Optional<EditorCommand> command = dispatcher.dispatch(
                EditorInputEvent.characterTyped('(', 0, 0, java.util.Set.of()), context());

        assertTrue(command.isPresent());
        assertEquals("cmd-(", command.get().name());
    }

    @Test
    void dispatchReturnsEmptyWhenNoStrategyApplies() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(claiming("(", SmartEditPriority.NORMAL));
        EditorInputDispatcher dispatcher = new EditorInputDispatcher(registry);

        Optional<EditorCommand> command = dispatcher.dispatch(
                EditorInputEvent.characterTyped('a', 0, 0, java.util.Set.of()), context());

        assertTrue(command.isEmpty());
    }

    @Test
    void dispatchHandlesNullArguments() {
        EditorInputDispatcher dispatcher = new EditorInputDispatcher(new SmartEditingRegistry());
        assertTrue(dispatcher.dispatch(null, context()).isEmpty());
        assertTrue(dispatcher.dispatch(
                EditorInputEvent.characterTyped('a', 0, 0, java.util.Set.of()), null).isEmpty());
    }

    @Test
    void dispatchRejectsNullRegistry() {
        assertThrows(IllegalArgumentException.class, () -> new EditorInputDispatcher(null));
    }
}
