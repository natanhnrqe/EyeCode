package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MonacoCompletionProtocolTest {
    @Test
    void requestCarriesEditorCoordinatesAndTriggerReason() {
        MonacoCompletionRequest request = new MonacoCompletionRequest(
                "file:///C:/Main.java", 7, 4, 13,
                MonacoCompletionRequest.TriggerKind.INVOKED, null);

        assertEquals("file:///C:/Main.java", request.modelId());
        assertEquals(7, request.modelVersion());
        assertEquals(MonacoCompletionRequest.TriggerKind.INVOKED, request.triggerKind());
    }

    @Test
    void itemPreservesEyeCodeMetadataAndReplacementRange() {
        CompletionItem source = CompletionItem.builder("String", "String", CompletionItemKind.CLASS)
                .detail("java.lang.String")
                .documentation("Text value")
                .priority(4)
                .build();

        MonacoCompletionItem item = MonacoCompletionItem.from(source, 10, 13);

        assertEquals("String", item.label());
        assertEquals("java.lang.String", item.detail());
        assertEquals(10, item.replaceStart());
        assertEquals(13, item.replaceEnd());
        assertEquals(4, item.sortKey());
    }
}
