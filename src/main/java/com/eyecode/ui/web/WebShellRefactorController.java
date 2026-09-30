package com.eyecode.ui.web;

import com.eyecode.language.java.lsp.JdtLsProjectService;
import com.eyecode.language.refactor.PrepareRenameResult;
import com.eyecode.language.refactor.RenamePlan;
import com.eyecode.language.refactor.RenameResult;
import com.eyecode.project.ProjectLifecycleService;
import com.eyecode.project.ProjectRenameService;
import com.eyecode.project.model.ProjectModel;
import com.eyecode.ui.web.monaco.MonacoModelId;
import com.eyecode.workbench.editor.EditorManager;
import com.eyecode.workbench.editor.EditorSession;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WebShellRefactorController {
    private final WebShellSurface surface;
    private final EditorManager manager;
    private final ProjectLifecycleService projects;
    private final JdtLsProjectService jdt;
    private final ProjectRenameService renameService;
    private volatile boolean disposed;

    public WebShellRefactorController(WebShellSurface surface, EditorManager manager,
                                      ProjectLifecycleService projects, JdtLsProjectService jdt,
                                      ProjectRenameService renameService) {
        this.surface = surface;
        this.manager = manager;
        this.projects = projects;
        this.jdt = jdt;
        this.renameService = renameService;
        surface.registerHandler("refactor", "prepareRename", this::prepareRename);
        surface.registerHandler("refactor", "rename", this::rename);
    }

    public void dispose() {
        disposed = true;
    }

    private WebShellEnvelope prepareRename(WebShellEnvelope message) {
        if (disposed) return message.response(Map.of("supported", false));
        SessionContent content = sessionContent(message.payload());
        ProjectModel project = projects.currentProject();
        if (content == null || project == null) return message.response(Map.of("supported", false));
        int offset = offset(message.payload(), content.source());
        if (offset < 0) return message.response(Map.of("supported", false));
        PrepareRenameResult result = jdt.prepareRename(content.file(), content.source(), content.version(), offset)
                .orElse(null);
        if (result == null) return message.response(Map.of("supported", false));
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("startLine", result.line() + 1);
        range.put("startColumn", result.character() + 1);
        int endLine = contentSourceLines(result, content).endLine;
        int endColumn = contentSourceLines(result, content).endColumn;
        range.put("endLine", endLine);
        range.put("endColumn", endColumn);
        return message.response(Map.of("supported", true, "range", range, "placeholder", result.placeholder()));
    }

    private WebShellEnvelope rename(WebShellEnvelope message) {
        if (disposed) return message.response(renamePayload(new RenameResult(false, 0, List.of())));
        SessionContent content = sessionContent(message.payload());
        ProjectModel project = projects.currentProject();
        if (content == null || project == null) {
            return message.response(renamePayload(new RenameResult(false, 0, List.of())));
        }
        String newName = WebShellPayload.text(message.payload(), "newName").trim();
        try {
            renameService.requireValidIdentifier(newName);
        } catch (IllegalArgumentException exception) {
            return message.error(new WebShellError("INVALID_IDENTIFIER", exception.getMessage(), false));
        }
        int offset = offset(message.payload(), content.source());
        if (offset < 0) return message.response(renamePayload(new RenameResult(false, 0, List.of())));
        var plan = jdt.rename(content.file(), content.source(), content.version(), offset, newName).orElse(null);
        if (plan == null || plan.isEmpty()) {
            return message.response(renamePayload(new RenameResult(false, 0, List.of())));
        }
        RenameResult result = renameService.rename(project, plan);
        if (!result.success() && result.failedFiles().isEmpty()) {
            return message.error(new WebShellError("RENAME_NO_CHANGE", "Nenhuma alteracao produzida pelo rename", true));
        }
        return message.response(renamePayload(result));
    }

    private static Map<String, Object> renamePayload(RenameResult result) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("success", result.success());
        payload.put("applied", result.applied());
        payload.put("failedFiles", result.failedFiles());
        return payload;
    }

    private SessionContent sessionContent(Map<String, Object> payload) {
        String modelId = WebShellPayload.text(payload, "uri");
        EditorSession session = null;
        for (EditorSession candidate : manager.getSessions()) {
            if (MonacoModelId.matches(modelId, candidate.getFile()) || MonacoModelId.forSession(candidate).equals(modelId)) {
                session = candidate;
                break;
            }
        }
        if (session == null || session.getFile() == null) return null;
        var snapshot = manager.getBuffer(session.getSessionId())
                .map(buffer -> buffer.getDocument().snapshot()).orElse(null);
        if (snapshot == null) return null;
        return new SessionContent(session.getFile(), snapshot.getText(), snapshot.version());
    }

    private int offset(Map<String, Object> payload, String source) {
        int offset = (int) WebShellPayload.number(payload, "offset", -1);
        if (offset >= 0) return Math.min(offset, source.length());
        int line = (int) WebShellPayload.number(payload, "line", 0);
        int column = (int) WebShellPayload.number(payload, "column", 0);
        if (line < 1) return -1;
        var lines = com.eyecode.editor.intelligence.document.LineMap.of(source);
        int index = Math.min(line - 1, Math.max(0, lines.lineCount() - 1));
        int columnZero = Math.max(0, column - 1);
        int maxColumn = lines.lineEndOffset(index) - lines.lineStartOffset(index);
        columnZero = Math.min(columnZero, maxColumn);
        return lines.offsetOf(index, columnZero);
    }

    private static record SessionContent(java.nio.file.Path file, String source, long version) {}

    private record EndCoordinates(int endLine, int endColumn) {}

    private static EndCoordinates contentSourceLines(PrepareRenameResult result, SessionContent content) {
        var lines = com.eyecode.editor.intelligence.document.LineMap.of(content.source());
        int endOffset = Math.min(result.endOffset(), content.source().length());
        int line = lines.lineOfOffset(endOffset);
        int column = lines.columnOfOffset(endOffset);
        return new EndCoordinates(line + 1, column + 1);
    }
}
