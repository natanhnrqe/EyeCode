package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.SmartEditingRegistry;

/**
 * Registers the Sprint 5.1b delimiter strategies, the Sprint 5.1c smart enter /
 * auto-indent strategies and the Sprint 5.1d smart navigation strategies
 * (Home/End/Backspace/Delete) with their explicit priorities.
 * <p>
 * Order matters: {@link ClosingDelimiterStrategy} and {@link SmartEnterStrategy}
 * run first at {@code HIGH} (skip-over and smart-enter win over everything
 * else); {@code NORMAL} strategies then apply in registration order — opening
 * delimiters, quotes, the general auto-indent fallback for plain Enter, and the
 * 5.1d navigation keys, which never overlap because they claim disjoint keys
 * (HOME, END, BACKSPACE, DELETE vs typed delimiters / ENTER).
 */
public final class SmartEditingStrategies {

    private SmartEditingStrategies() {
    }

    public static SmartEditingRegistry defaultRegistry() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        registry.register(new ClosingDelimiterStrategy());
        registry.register(new SmartEnterStrategy());
        registry.register(new OpeningDelimiterStrategy());
        registry.register(new QuoteCompletionStrategy());
        registry.register(new AutoIndentStrategy());
        registry.register(new SmartHomeStrategy());
        registry.register(new SmartEndStrategy());
        registry.register(new SmartBackspaceStrategy());
        registry.register(new SmartDeleteStrategy());
        return registry;
    }
}
