package com.eyecode.editor.intelligence.pipeline.strategy;

import com.eyecode.editor.intelligence.pipeline.SmartEditingRegistry;
import com.eyecode.editor.intelligence.selection.JavaSelectionExpander;
import com.eyecode.editor.intelligence.selection.SelectionHistory;

/**
 * Registers the Sprint 5.1b delimiter strategies, the Sprint 5.1c smart enter /
 * auto-indent strategies, the Sprint 5.1d smart navigation strategies
 * (Home/End/Backspace/Delete) and the Sprint 5.1e selection expansion
 * strategies (Ctrl+W / Ctrl+Shift+W) with their explicit priorities.
 * <p>
 * Order matters: {@link ClosingDelimiterStrategy} and {@link SmartEnterStrategy}
 * run first at {@code HIGH} (skip-over and smart-enter win over everything
 * else); {@code NORMAL} strategies then apply in registration order — opening
 * delimiters, quotes, the general auto-indent fallback for plain Enter, the
 * 5.1d navigation keys, and the 5.1e expand/shrink pair, which never overlap
 * because they claim disjoint keys (Ctrl+W / Ctrl+Shift+W vs typed delimiters,
 * ENTER, HOME, END, BACKSPACE, DELETE).
 * <p>
 * {@link ExtendSelectionStrategy} and {@link ShrinkSelectionStrategy} share a
 * single {@link SelectionHistory} per registry so shrink can walk back through
 * the ranges recorded by expand.
 */
public final class SmartEditingStrategies {

    private SmartEditingStrategies() {
    }

    public static SmartEditingRegistry defaultRegistry() {
        SmartEditingRegistry registry = new SmartEditingRegistry();
        SelectionHistory selectionHistory = new SelectionHistory();
        registry.register(new ClosingDelimiterStrategy());
        registry.register(new SmartEnterStrategy());
        registry.register(new OpeningDelimiterStrategy());
        registry.register(new QuoteCompletionStrategy());
        registry.register(new AutoIndentStrategy());
        registry.register(new SmartHomeStrategy());
        registry.register(new SmartEndStrategy());
        registry.register(new SmartBackspaceStrategy());
        registry.register(new SmartDeleteStrategy());
        registry.register(new ExtendSelectionStrategy(new JavaSelectionExpander(), selectionHistory));
        registry.register(new ShrinkSelectionStrategy(selectionHistory));
        return registry;
    }
}
