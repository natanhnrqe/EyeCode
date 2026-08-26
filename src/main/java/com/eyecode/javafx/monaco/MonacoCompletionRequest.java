package com.eyecode.javafx.monaco;

public record MonacoCompletionRequest(
        String modelId,
        long modelVersion,
        int line,
        int column,
        TriggerKind triggerKind,
        String triggerCharacter,
        long requestId,
        boolean explicit
) {
    public enum TriggerKind { INVOKED, TRIGGER_CHARACTER, INCOMPLETE }

    public MonacoCompletionRequest(String modelId, long modelVersion, int line, int column,
                                   TriggerKind triggerKind, String triggerCharacter) {
        this(modelId, modelVersion, line, column, triggerKind, triggerCharacter, 0L);
    }

    public MonacoCompletionRequest(String modelId, long modelVersion, int line, int column,
                                   TriggerKind triggerKind, String triggerCharacter,
                                   long requestId) {
        this(modelId, modelVersion, line, column, triggerKind, triggerCharacter, requestId, false);
    }

    public MonacoCompletionRequest {
        modelId = modelId == null ? "" : modelId;
        triggerKind = triggerKind == null ? TriggerKind.INVOKED : triggerKind;
    }
}
