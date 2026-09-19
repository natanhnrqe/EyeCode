package com.eyecode.ui.web;

import com.eyecode.editor.v2.EditorDocument;
import com.eyecode.workbench.editor.EditorSession;
import com.eyecode.ui.web.monaco.MonacoModelId;

import java.util.LinkedHashMap;
import java.util.Map;

public record WebDocumentSnapshot(
        String uri,
        String displayName,
        String language,
        String content,
        long version,
        boolean dirty,
        boolean readOnly,
        String kind
) {
    public Map<String, Object> payload() {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("uri", uri);
        value.put("displayName", displayName);
        value.put("language", language);
        value.put("content", content);
        value.put("version", version);
        value.put("dirty", dirty);
        value.put("readOnly", readOnly);
        value.put("kind", kind);
        return value;
    }

    public static WebDocumentSnapshot file(EditorSession session, EditorDocument document, String language) {
        return new WebDocumentSnapshot(
                MonacoModelId.forSession(session),
                session.getDisplayName(),
                language,
                document.snapshot().getText(),
                document.currentVersion(),
                document.isDirty(),
                false,
                "file");
    }

    public static WebDocumentSnapshot untitled(EditorSession session, EditorDocument document,
                                               String displayName, String language) {
        return new WebDocumentSnapshot(
                MonacoModelId.forSession(session),
                displayName,
                language,
                document.snapshot().getText(),
                document.currentVersion(),
                document.isDirty(),
                false,
                "file");
    }
}
