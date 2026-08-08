package com.eyecode.editor.intelligence.pipeline;

/**
 * Consultation order for {@link SmartEditStrategy} instances.
 * <p>
 * {@code HIGH} strategies are consulted first. Priorities resolve conflicts
 * between future strategies such as brace completion, quote completion,
 * auto-indent, autocomplete and snippets.
 */
public enum SmartEditPriority {

    HIGH,
    NORMAL,
    LOW;

    public int rank() {
        return ordinal();
    }
}
