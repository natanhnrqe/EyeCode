package com.eyecode.language.java.lsp;

import com.eyecode.editor.intelligence.document.LineMap;
import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import com.eyecode.language.diagnostics.QuickFix;
import com.eyecode.language.diagnostics.QuickFixEdit;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionContext;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class JdtLsProjectDiagnostics {

    public interface Listener {
        void onJdtPublish(String uri, List<Diagnostic> diagnostics);
    }

    private final JdtLsDocumentSync documents;
    private volatile JdtLsSession session;
    private volatile Listener listener;

    JdtLsProjectDiagnostics(JdtLsDocumentSync documents) {
        this.documents = Objects.requireNonNull(documents, "documents");
    }

    void attach(JdtLsSession session) {
        this.session = session;
        if (session != null) session.setDiagnosticsListener(this::onServerPublish);
    }

    void detach() {
        JdtLsSession current = session;
        session = null;
        if (current != null) current.setDiagnosticsListener(null);
    }

    public void setListener(Listener value) {
        this.listener = value;
    }

    void onServerPublish(String uri, List<org.eclipse.lsp4j.Diagnostic> diagnostics) {
        if (uri == null || uri.isBlank()) return;
        publishSynced(uri, diagnostics, documents.lastSyncedVersion(uri));
    }

    void publishSynced(String uri, List<org.eclipse.lsp4j.Diagnostic> diagnostics, long syncedVersion) {
        if (uri == null || uri.isBlank() || syncedVersion < 0) return;
        List<Diagnostic> converted = clamp(convertAll(diagnostics), documents.textOf(uri));
        Listener current = listener;
        if (current == null) return;
        try {
            current.onJdtPublish(uri, converted);
        } catch (RuntimeException ignored) {
        }
    }

    public synchronized List<QuickFix> quickFixes(String uri, int startLine, int startColumn, int endLine, int endColumn) {
        JdtLsSession current = session;
        if (current == null || current.state() != JdtLsLifecycleState.READY || !current.supportsCodeAction()) {
            return List.of();
        }
        try {
            List<org.eclipse.lsp4j.Diagnostic> context = contextDiagnostics(current, uri, startLine, startColumn, endLine, endColumn);
            if (context.isEmpty()) return List.of();
            List<Either<Command, CodeAction>> result = current.codeAction(uri,
                    range(startLine, startColumn, endLine, endColumn),
                    new CodeActionContext(context, List.of("quickfix")));
            return toQuickFixes(uri, result);
        } catch (RuntimeException exception) {
            return List.of();
        }
    }

    static List<Diagnostic> convertAll(List<org.eclipse.lsp4j.Diagnostic> diagnostics) {
        if (diagnostics == null || diagnostics.isEmpty()) return List.of();
        List<Diagnostic> converted = new ArrayList<>(diagnostics.size());
        for (org.eclipse.lsp4j.Diagnostic item : diagnostics) {
            if (item == null) continue;
            converted.add(toDiagnostic(item));
        }
        return List.copyOf(converted);
    }

    static Diagnostic toDiagnostic(org.eclipse.lsp4j.Diagnostic item) {
        Range span = item.getRange();
        int startLine = line(span == null ? null : span.getStart()) + 1;
        int startColumn = character(span == null ? null : span.getStart()) + 1;
        int endLine = line(span == null ? null : span.getEnd()) + 1;
        int endColumn = character(span == null ? null : span.getEnd()) + 1;
        return new Diagnostic(severity(item.getSeverity()), code(item.getCode()), message(item.getMessage()),
                startLine, startColumn, endLine, endColumn, "jdt");
    }

    static DiagnosticSeverity severity(org.eclipse.lsp4j.DiagnosticSeverity severity) {
        if (severity == null) return DiagnosticSeverity.HINT;
        return switch (severity) {
            case Error -> DiagnosticSeverity.ERROR;
            case Warning -> DiagnosticSeverity.WARNING;
            case Information -> DiagnosticSeverity.INFO;
            case Hint -> DiagnosticSeverity.HINT;
        };
    }

    static String code(Either<String, Integer> code) {
        if (code == null) return "";
        return code.isLeft() ? code.getLeft() : code.getRight() == null ? "" : String.valueOf(code.getRight());
    }

    static String message(Either<String, org.eclipse.lsp4j.MarkupContent> message) {
        if (message == null) return "";
        return message.isLeft() ? message.getLeft() : message.getRight() == null ? "" : message.getRight().getValue();
    }

    static List<QuickFix> toQuickFixes(String uri, List<Either<Command, CodeAction>> result) {
        if (result == null || result.isEmpty()) return List.of();
        List<QuickFix> fixes = new ArrayList<>(result.size());
        for (Either<Command, CodeAction> entry : result) {
            QuickFix fix = toQuickFix(uri, entry);
            if (fix != null) fixes.add(fix);
        }
        return List.copyOf(fixes);
    }

    static QuickFix toQuickFix(String uri, Either<Command, CodeAction> entry) {
        if (entry == null) return null;
        return entry.isLeft() ? toQuickFix(uri, entry.getLeft()) : toQuickFix(uri, entry.getRight());
    }

    static QuickFix toQuickFix(String uri, CodeAction action) {
        if (action == null) return null;
        String title = action.getTitle();
        if (title == null || title.isBlank()) return null;
        if (action.getDisabled() != null) return null;
        String kind = action.getKind();
        if (kind != null && !kind.isBlank() && !kind.startsWith("quickfix")) return null;
        WorkspaceEdit edit = action.getEdit();
        if (edit != null) {
            QuickFix fix = quickFix(uri, title, kind, editsFor(uri, edit.getChanges()));
            if (fix != null) return fix;
        }
        return quickFix(uri, title, kind, action.getCommand());
    }

    static QuickFix toQuickFix(String uri, Command command) {
        if (command == null) return null;
        String title = command.getTitle();
        if (title == null || title.isBlank()) return null;
        return quickFix(uri, title, "quickfix", commandEdits(command, uri));
    }

    private static QuickFix quickFix(String uri, String title, String kind, Command command) {
        if (command == null || command.getCommand() == null || !command.getCommand().startsWith("java.apply")) {
            return null;
        }
        return quickFix(uri, title, kind, commandEdits(command, uri));
    }

    private static QuickFix quickFix(String uri, String title, String kind, List<TextEdit> edits) {
        if (edits == null || edits.isEmpty()) return null;
        List<QuickFixEdit> converted = new ArrayList<>(edits.size());
        for (TextEdit edit : edits) {
            if (edit == null || edit.getRange() == null) continue;
            Range span = edit.getRange();
            converted.add(new QuickFixEdit(
                    line(span.getStart()) + 1, character(span.getStart()) + 1,
                    line(span.getEnd()) + 1, character(span.getEnd()) + 1,
                    edit.getNewText()));
        }
        if (converted.isEmpty()) return null;
        return new QuickFix(title, kind, converted);
    }

    private static List<TextEdit> commandEdits(Command command, String uri) {
        List<Object> arguments = command.getArguments();
        if (arguments == null || arguments.isEmpty()) return List.of();
        Object first = arguments.get(0);
        if (first instanceof WorkspaceEdit edit) return editsFor(uri, edit.getChanges());
        if (first instanceof Map<?, ?> raw) return editsFor(uri, rawChanges(raw));
        return List.of();
    }

    private static List<TextEdit> editsFor(String uri, Map<String, List<TextEdit>> changes) {
        if (changes == null) return List.of();
        return changes.get(uri);
    }

    private static Map<String, List<TextEdit>> rawChanges(Map<?, ?> edit) {
        Object changes = edit.get("changes");
        if (!(changes instanceof Map<?, ?> byUri)) return Map.of();
        Map<String, List<TextEdit>> converted = new java.util.LinkedHashMap<>();
        byUri.forEach((key, value) -> {
            if (value instanceof List<?> items) {
                List<TextEdit> parsed = new ArrayList<>(items.size());
                for (Object item : items) {
                    TextEdit textEdit = rawTextEdit(item);
                    if (textEdit != null) parsed.add(textEdit);
                }
                converted.put(String.valueOf(key), List.copyOf(parsed));
            }
        });
        return converted;
    }

    private static TextEdit rawTextEdit(Object value) {
        if (!(value instanceof Map<?, ?> map)) return null;
        Object span = map.get("range");
        Object newText = map.get("newText");
        if (!(span instanceof Map<?, ?> range) || !(newText instanceof String text)) return null;
        Position start = rawPosition(range.get("start"));
        Position end = rawPosition(range.get("end"));
        if (start == null || end == null) return null;
        return new TextEdit(new Range(start, end), text);
    }

    private static Position rawPosition(Object value) {
        if (!(value instanceof Map<?, ?> map)) return null;
        Object line = map.get("line");
        Object character = map.get("character");
        if (!(line instanceof Number lineNumber) || !(character instanceof Number column)) return null;
        return new Position(lineNumber.intValue(), column.intValue());
    }

    private static List<org.eclipse.lsp4j.Diagnostic> contextDiagnostics(JdtLsSession session, String uri, int startLine,
                                                                         int startColumn, int endLine, int endColumn) {
        List<org.eclipse.lsp4j.Diagnostic> published = session.publishedDiagnostics(uri);
        if (published.isEmpty()) return List.of();
        Range window = range(startLine, startColumn, endLine, endColumn);
        List<org.eclipse.lsp4j.Diagnostic> intersecting = new ArrayList<>();
        for (org.eclipse.lsp4j.Diagnostic diagnostic : published) {
            if (diagnostic != null && intersects(diagnostic.getRange(), window)) intersecting.add(diagnostic);
        }
        return List.copyOf(intersecting);
    }

    private static boolean intersects(Range first, Range second) {
        if (first == null || second == null) return true;
        boolean before = line(first.getEnd()) < line(second.getStart())
                || (line(first.getEnd()) == line(second.getStart()) && character(first.getEnd()) < character(second.getStart()));
        boolean after = line(first.getStart()) > line(second.getEnd())
                || (line(first.getStart()) == line(second.getEnd()) && character(first.getStart()) > character(second.getEnd()));
        return !before && !after;
    }

    static Range range(int startLine, int startColumn, int endLine, int endColumn) {
        return new Range(new Position(Math.max(0, startLine - 1), Math.max(0, startColumn - 1)),
                new Position(Math.max(0, endLine - 1), Math.max(0, endColumn - 1)));
    }

    static List<Diagnostic> clamp(List<Diagnostic> diagnostics, String text) {
        if (diagnostics.isEmpty() || text == null || text.isEmpty()) return diagnostics;
        LineMap lines = LineMap.of(text);
        List<Diagnostic> clamped = new ArrayList<>(diagnostics.size());
        for (Diagnostic diagnostic : diagnostics) {
            clamped.add(clamp(diagnostic, lines));
        }
        return List.copyOf(clamped);
    }

    private static Diagnostic clamp(Diagnostic diagnostic, LineMap lines) {
        int lastLine = lines.lineCount();
        if (diagnostic.startLine() <= lastLine && diagnostic.endLine() <= lastLine) return diagnostic;
        return new Diagnostic(diagnostic.severity(), diagnostic.code(), diagnostic.message(),
                Math.min(diagnostic.startLine(), lastLine), diagnostic.startColumn(),
                Math.min(diagnostic.endLine(), lastLine), diagnostic.endColumn(), diagnostic.category());
    }

    private static int line(Position position) {
        return position == null || position.getLine() < 0 ? 0 : position.getLine();
    }

    private static int character(Position position) {
        return position == null || position.getCharacter() < 0 ? 0 : position.getCharacter();
    }
}
