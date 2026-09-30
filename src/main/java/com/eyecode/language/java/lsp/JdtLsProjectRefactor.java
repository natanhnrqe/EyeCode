package com.eyecode.language.java.lsp;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.language.refactor.PrepareRenameResult;
import com.eyecode.language.refactor.RenamePlan;
import org.eclipse.lsp4j.PrepareRenameDefaultBehavior;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.messages.Either3;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class JdtLsProjectRefactor {
    private final JdtLsSession session;
    private final JdtLsDocumentSync documents = new JdtLsDocumentSync();
    private final Path projectRoot;

    public JdtLsProjectRefactor(JdtLsSession session, Path projectRoot) {
        this.session = session;
        this.projectRoot = projectRoot == null ? null : projectRoot.toAbsolutePath().normalize();
    }

    public synchronized Optional<PrepareRenameResult> prepareRename(Path file, String source, long version,
                                                                    int caretOffset, Duration timeout) {
        if (file == null || projectRoot == null || session.state() != JdtLsLifecycleState.READY
                || !session.supportsRename()) return Optional.empty();
        try {
            String uri = documents.synchronize(session, file, source, version);
            LineMap lines = LineMap.of(source);
            Optional<PrepareRenameResult> result = prepareAt(uri, lines, caretOffset, source, timeout);
            if (result.isEmpty()) {
                for (int candidate = caretOffset - 1; result.isEmpty() && candidate >= Math.max(0, caretOffset - 4); candidate--) {
                    result = prepareAt(uri, lines, candidate, source, timeout);
                }
            }
            return result;
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public synchronized Optional<RenamePlan> rename(Path file, String source, long version,
                                                    int caretOffset, String newName, Duration timeout) {
        if (file == null || projectRoot == null || session.state() != JdtLsLifecycleState.READY
                || !session.supportsRename()) return Optional.empty();
        try {
            String uri = documents.synchronize(session, file, source, version);
            LineMap lines = LineMap.of(source);
            WorkspaceEdit edit = tryRename(uri, lines, caretOffset, newName, timeout);
            if (edit == null) {
                for (int candidate = caretOffset - 1; edit == null && candidate >= Math.max(0, caretOffset - 4); candidate--) {
                    edit = tryRename(uri, lines, candidate, newName, timeout);
                }
            }
            return edit == null ? Optional.empty() : toPlan(edit, projectRoot);
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public synchronized void close(Path file) {
        documents.close(session, file);
    }

    private Optional<PrepareRenameResult> prepareAt(String uri, LineMap lines, int offset, String source, Duration timeout) {
        Either3<Range, org.eclipse.lsp4j.PrepareRenameResult, PrepareRenameDefaultBehavior> result =
                session.prepareRename(uri, lines.lineOfOffset(offset), lines.columnOfOffset(offset), timeout);
        if (result == null) return Optional.empty();
        Range range = null;
        String placeholder = null;
        if (result.isFirst()) {
            range = result.getFirst();
        } else if (result.isSecond()) {
            range = result.getSecond().getRange();
            placeholder = result.getSecond().getPlaceholder();
        } else if (result.isThird()) {
            return Optional.empty();
        }
        if (range == null) return Optional.empty();
        int start = safeOffset(lines, range.getStart().getLine(), range.getStart().getCharacter(), source.length());
        int end = safeOffset(lines, range.getEnd().getLine(), range.getEnd().getCharacter(), source.length());
        if (end < start) return Optional.empty();
        String name = placeholder == null || placeholder.isBlank() ? source.substring(start, end) : placeholder;
        return Optional.of(new PrepareRenameResult(start, end, name,
                range.getStart().getLine(), range.getStart().getCharacter()));
    }

    private WorkspaceEdit tryRename(String uri, LineMap lines, int offset, String newName, Duration timeout) {
        return session.rename(uri, lines.lineOfOffset(offset), lines.columnOfOffset(offset), newName, timeout);
    }

    static Optional<RenamePlan> toPlan(WorkspaceEdit edit, Path projectRoot) {
        if (edit == null) return Optional.empty();
        Map<Path, List<RenamePlan.Edit>> grouped = new LinkedHashMap<>();
        if (edit.getChanges() != null) {
            for (Map.Entry<String, List<TextEdit>> entry : edit.getChanges().entrySet()) {
                addEdits(grouped, entry.getKey(), entry.getValue());
            }
        }
        if (edit.getDocumentChanges() != null) {
            for (org.eclipse.lsp4j.jsonrpc.messages.Either<org.eclipse.lsp4j.TextDocumentEdit, org.eclipse.lsp4j.ResourceOperation> change
                    : edit.getDocumentChanges()) {
                if (change != null && change.isLeft()) {
                    addDocumentEdits(grouped, change.getLeft().getTextDocument().getUri(), change.getLeft().getEdits());
                }
            }
        }
        if (grouped.isEmpty()) return Optional.empty();
        Path root = projectRoot.toAbsolutePath().normalize();
        for (Path file : grouped.keySet()) {
            if (!file.startsWith(root)) return Optional.empty();
        }
        return Optional.of(new RenamePlan(grouped));
    }

    private static void addDocumentEdits(Map<Path, List<RenamePlan.Edit>> grouped, String uri,
                                         List<Either<TextEdit, org.eclipse.lsp4j.SnippetTextEdit>> edits) {
        if (edits == null) return;
        addEdits(grouped, uri, edits.stream().filter(Either::isLeft).map(Either::getLeft).toList());
    }

    private static void addEdits(Map<Path, List<RenamePlan.Edit>> grouped, String uri, List<? extends TextEdit> edits) {
        if (uri == null || edits == null) return;
        Path file = fileOf(uri);
        if (file == null) return;
        List<RenamePlan.Edit> target = grouped.computeIfAbsent(file, ignored -> new ArrayList<>());
        for (TextEdit edit : edits) {
            if (edit == null || edit.getRange() == null) continue;
            target.add(new RenamePlan.Edit(
                    edit.getRange().getStart().getLine(), edit.getRange().getStart().getCharacter(),
                    edit.getRange().getEnd().getLine(), edit.getRange().getEnd().getCharacter(),
                    edit.getNewText() == null ? "" : edit.getNewText()));
        }
    }

    private static Path fileOf(String uri) {
        try {
            if (!uri.startsWith("file:")) return null;
            return Path.of(URI.create(uri)).toAbsolutePath().normalize();
        } catch (RuntimeException exception) {
            return null;
        }
    }

    private static int safeOffset(LineMap lines, int line, int column, int limit) {
        try {
            int safeLine = Math.max(0, Math.min(line, Math.max(0, lines.lineCount() - 1)));
            int maxColumn = lines.lineEndOffset(safeLine) - lines.lineStartOffset(safeLine);
            int offset = lines.offsetOf(safeLine, Math.max(0, Math.min(column, maxColumn)));
            return Math.max(0, Math.min(offset, limit));
        } catch (RuntimeException exception) {
            return limit;
        }
    }
}
