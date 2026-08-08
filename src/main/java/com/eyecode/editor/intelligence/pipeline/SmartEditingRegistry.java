package com.eyecode.editor.intelligence.pipeline;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Priority-ordered registry of {@link SmartEditStrategy} instances.
 * <p>
 * Strategies are consulted in priority order ({@link SmartEditPriority#HIGH}
 * first). Registration is idempotent per strategy instance.
 */
public final class SmartEditingRegistry {

    private final List<SmartEditStrategy> strategies = new ArrayList<>();

    public void register(SmartEditStrategy strategy) {
        if (strategy == null || strategies.contains(strategy)) {
            return;
        }
        strategies.add(strategy);
        strategies.sort(Comparator.comparingInt(s -> s.priority().rank()));
    }

    public void unregister(SmartEditStrategy strategy) {
        strategies.remove(strategy);
    }

    public void clear() {
        strategies.clear();
    }

    public List<SmartEditStrategy> strategies() {
        return List.copyOf(strategies);
    }
}
