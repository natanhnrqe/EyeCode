package com.eyecode.editor.intelligence.pipeline;

import java.util.Optional;

/**
 * Pluggable smart editing rule consulted by the {@link SmartEditingRegistry}.
 * <p>
 * A strategy declares whether an input belongs to it via {@link #supports} and,
 * when it does, builds the {@link EditorCommand} that implements the edit. Real
 * strategies (brace completion, quote completion, auto-indent, smart enter,
 * ...) register themselves in later sprints.
 */
public interface SmartEditStrategy {

    SmartEditPriority priority();

    boolean supports(EditorInputEvent event, EditorCommandContext context);

    Optional<EditorCommand> createCommand(EditorInputEvent event, EditorCommandContext context);
}
