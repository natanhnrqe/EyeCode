package com.eyecode.javafx.monaco;

import com.eyecode.editor.v2.completion.CompletionItem;
import com.eyecode.editor.v2.completion.CompletionItemKind;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
    void requestCarriesLatestRequestIdentityAndExplicitInvocation() {
        MonacoCompletionRequest request = new MonacoCompletionRequest(
                "file:///C:/Main.java", 9, 2, 4,
                MonacoCompletionRequest.TriggerKind.INVOKED, null, 42L, true);

        assertEquals(42L, request.requestId());
        assertTrue(request.explicit());
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
        assertEquals("String", item.filterText());
        assertTrue(!item.snippet());
    }

    @Test
    void completionRequestJsonCarriesExplicitTriggerAndIdentity() {
        MonacoCompletionRequest request = JavaFxMonacoEditorSurface.parseCompletionRequestForTest(
                "{\"kind\":\"completion\",\"id\":\"file:///C:/Main.java\","
                        + "\"version\":7,\"line\":2,\"column\":4,\"offset\":15,"
                        + "\"replaceStart\":12,\"replaceEnd\":15,\"content\":\"String value\","
                        + "\"triggerKind\":\"invoked\",\"requestId\":42,\"explicit\":true}");

        assertEquals(42L, request.requestId());
        assertTrue(request.explicit());
        assertEquals(2, request.line());
        assertEquals(15, request.caretOffset());
        assertEquals(12, request.replaceStart());
        assertEquals(15, request.replaceEnd());
        assertEquals("String value", request.content());
    }

    @Test
    void snippetItemsPreserveMonacoSnippetMetadata() {
        CompletionItem source = CompletionItem.builder("sout", "System.out.println(${0});", CompletionItemKind.SNIPPET)
                .build();

        MonacoCompletionItem item = MonacoCompletionItem.from(source, 0, 4);

        assertTrue(item.snippet());
        assertEquals("sout", item.filterText());
    }

    @Test
    void completionResponseJsonIncludesMonacoFilterAndSnippetFields() {
        CompletionItem source = CompletionItem.builder("sout", "System.out.println(${0});", CompletionItemKind.SNIPPET)
                .build();
        String json = JavaFxMonacoEditorSurface.commandJsonForTest(new MonacoCommand.CompletionResponse(
                "file:///C:/Main.java", 4, List.of(MonacoCompletionItem.from(source, 0, 4))));

        assertTrue(json.contains("\"filterText\":\"sout\""));
        assertTrue(json.contains("\"snippet\":true"));
    }
}
