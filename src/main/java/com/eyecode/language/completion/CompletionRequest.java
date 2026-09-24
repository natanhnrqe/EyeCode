package com.eyecode.language.completion;

import com.eyecode.language.LanguageDocument;

public record CompletionRequest(LanguageDocument document, long version, String source, int caretOffset,
                                boolean explicit, int replaceStart, int replaceEnd,
                                TriggerKind triggerKind, String triggerCharacter) {
    public enum TriggerKind { INVOKED, TRIGGER_CHARACTER, INCOMPLETE }

    public CompletionRequest(LanguageDocument document, long version, String source, int caretOffset,
                             boolean explicit, int replaceStart, int replaceEnd) {
        this(document, version, source, caretOffset, explicit, replaceStart, replaceEnd,
                TriggerKind.INVOKED, null);
    }

    public CompletionRequest {
        if (document == null) throw new IllegalArgumentException("document must not be null");
        source = source == null ? "" : source;
        caretOffset = Math.max(0, Math.min(caretOffset, source.length()));
        replaceStart = Math.max(-1, replaceStart);
        replaceEnd = Math.max(-1, replaceEnd);
        triggerKind = triggerKind == null ? TriggerKind.INVOKED : triggerKind;
        triggerCharacter = triggerCharacter == null || triggerCharacter.isBlank() ? null : triggerCharacter;
    }
}
