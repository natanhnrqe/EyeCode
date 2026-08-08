package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.SmartEditingRegistry;

/**
 * Registers the Sprint 5.1b delimiter strategies with their explicit priorities.
 * <p>
 * Order matters: {@link ClosingDelimiterStrategy} runs first at {@code HIGH} so
 * skip-over wins over any opening/quote behavior; {@code NORMAL} strategies then
 * apply in registration order (quotes before plain opening delimiters).
 */
public final class SmartEditingStrategies {

    private SmartEditingStrategies() {
    }

    public static SmartEditingRegistry defaultRegistry() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new ClosingDelimiterStrategy());
        registry.register(new QuoteCompletionStrategy());
        registry.register(new OpeningDelimiterStrategy());
        return registry;
    }
}
