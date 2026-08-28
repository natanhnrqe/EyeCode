package com.eyecode.javafx.monaco;

public record MonacoCompletionRequest(
        String modelId,
        long modelVersion,
        int line,
        int column,
        TriggerKind triggerKind,
        String triggerCharacter,
        long requestId,
        boolean explicit,
        int caretOffset,
        int replaceStart,
        int replaceEnd,
        String content
) {
    public enum TriggerKind { INVOKED, TRIGGER_CHARACTER, INCOMPLETE }

    public MonacoCompletionRequest(String modelId, long modelVersion, int line, int column,
                                   TriggerKind triggerKind, String triggerCharacter) {
        this(modelId, modelVersion, line, column, triggerKind, triggerCharacter, 0L, false);
    }

    public MonacoCompletionRequest(String modelId, long modelVersion, int line, int column,
                                   TriggerKind triggerKind, String triggerCharacter,
                                   long requestId) {
        this(modelId, modelVersion, line, column, triggerKind, triggerCharacter, requestId, false);
    }

    public MonacoCompletionRequest(String modelId, long modelVersion, int line, int column,
                                   TriggerKind triggerKind, String triggerCharacter,
                                   long requestId, boolean explicit) {
        this(modelId, modelVersion, line, column, triggerKind, triggerCharacter, requestId, explicit,
                -1, -1, -1, "");
    }

    public MonacoCompletionRequest {
        modelId = modelId == null ? "" : modelId;
        triggerKind = triggerKind == null ? TriggerKind.INVOKED : triggerKind;
        caretOffset = Math.max(-1, caretOffset);
        replaceStart = Math.max(-1, replaceStart);
        replaceEnd = Math.max(-1, replaceEnd);
        content = content == null ? "" : content;
    }

    public boolean hasSnapshot() {
        return caretOffset >= 0;
    }
}
