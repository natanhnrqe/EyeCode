package com.eyecode.javafx.monaco;

public record MonacoCompletionRequest(
        String modelId,
        long modelVersion,
        int line,
        int column,
        TriggerKind triggerKind,
        String triggerCharacter
) {
    public enum TriggerKind { INVOKED, TRIGGER_CHARACTER, INCOMPLETE }

    public MonacoCompletionRequest {
        modelId = modelId == null ? "" : modelId;
        triggerKind = triggerKind == null ? TriggerKind.INVOKED : triggerKind;
    }
}
