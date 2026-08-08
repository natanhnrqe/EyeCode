package com.eyecode.editor.intelligence.pipeline;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class SmartEditingRegistryTest {

    private static SmartEditStrategy strategy(SmartEditPriority priority) {
        return new SmartEditStrategy() {
            @Override
            public SmartEditPriority priority() {
                return priority;
            }

            @Override
            public boolean supports(EditorInputEvent event, EditorCommandContext context) {
                return false;
            }

            @Override
            public Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context) {
                return Optional.empty();
            }
        };
    }

    @Test
    void registerSortsByPriority() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        SmartEditStrategy low = strategy(SmartEditPriority.LOW);
        SmartEditStrategy high = strategy(SmartEditPriority.HIGH);
        SmartEditStrategy normal = strategy(SmartEditPriority.NORMAL);

        registry.register(low);
        registry.register(high);
        registry.register(normal);

        List<SmartEditStrategy> strategies = registry.strategies();
        assertEquals(List.of(high, normal, low), strategies);
    }

    @Test
    void registerIsIdempotentPerInstance() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        SmartEditStrategy strategy = strategy(SmartEditPriority.NORMAL);
        registry.register(strategy);
        registry.register(strategy);
        assertEquals(1, registry.strategies().size());
    }

    @Test
    void registerIgnoresNull() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(null);
        assertTrue(registry.strategies().isEmpty());
    }

    @Test
    void unregisterRemovesStrategy() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        SmartEditStrategy strategy = strategy(SmartEditPriority.NORMAL);
        registry.register(strategy);
        registry.unregister(strategy);
        assertTrue(registry.strategies().isEmpty());
    }

    @Test
    void clearRemovesEverything() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(strategy(SmartEditPriority.HIGH));
        registry.register(strategy(SmartEditPriority.LOW));
        registry.clear();
        assertTrue(registry.strategies().isEmpty());
    }

    @Test
    void strategiesReturnsSnapshot() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(strategy(SmartEditPriority.HIGH));
        List<SmartEditStrategy> snapshot = registry.strategies();
        registry.clear();
        assertEquals(1, snapshot.size());
    }

    @Test
    void priorityRankIsConsultationOrder() {
        assertEquals(0, SmartEditPriority.HIGH.rank());
        assertEquals(1, SmartEditPriority.NORMAL.rank());
        assertEquals(2, SmartEditPriority.LOW.rank());
    }
}
