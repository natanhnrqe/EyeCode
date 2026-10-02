package com.eyecode.language.java.lsp;

import com.eyecode.language.diagnostics.Diagnostic;
import com.eyecode.language.diagnostics.DiagnosticSeverity;
import com.eyecode.language.diagnostics.QuickFix;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.Position;
import org.eclipse.lsp4j.PublishDiagnosticsParams;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.services.LanguageClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JdtLsDiagnosticsMappingTest {
    private static final String URI = "file:///project/src/Main.java";

    @TempDir
    Path temporaryDirectory;

    @Test
    void documentChangesWorkspaceEditIsAcceptedAsQuickFixEdits() {
        org.eclipse.lsp4j.TextDocumentEdit documentEdit = new org.eclipse.lsp4j.TextDocumentEdit(
                new org.eclipse.lsp4j.VersionedTextDocumentIdentifier(URI, 7),
                List.of(Either.forLeft(new TextEdit(new Range(new Position(0, 0), new Position(0, 0)),
                        "import java.util.List;\n"))));
        WorkspaceEdit edit = new WorkspaceEdit();
        edit.setDocumentChanges(List.of(Either.forLeft(documentEdit)));
        CodeAction action = new CodeAction("Import 'List' (java.util)");
        action.setKind("quickfix");
        action.setEdit(edit);

        QuickFix fix = JdtLsProjectDiagnostics.toQuickFix(URI, action);

        assertEquals("Import 'List' (java.util)", fix.title());
        assertEquals(1, fix.edits().size());
        assertEquals("import java.util.List;\n", fix.edits().get(0).newText());
        assertEquals(1, fix.edits().get(0).startLine());
        assertEquals(1, fix.edits().get(0).startColumn());
    }

    @Test
    void documentChangesInRawCommandArgumentsAreAcceptedAsQuickFixEdits() {
        Map<String, Object> documentEdit = new LinkedHashMap<>();
        documentEdit.put("textDocument", Map.of("uri", URI, "version", 0));
        documentEdit.put("edits", List.of(Map.of(
                "range", Map.of(
                        "start", Map.of("line", 0, "character", 0),
                        "end", Map.of("line", 0, "character", 0)),
                "newText", "import java.io.FileReader;\n")));
        Command command = new Command("Import 'FileReader'", "java.apply.workspaceEdit");
        command.setArguments(List.of(Map.of("documentChanges", List.of(documentEdit))));

        QuickFix fix = JdtLsProjectDiagnostics.toQuickFix(URI, command);

        assertEquals("Import 'FileReader'", fix.title());
        assertEquals(1, fix.edits().size());
        assertEquals("import java.io.FileReader;\n", fix.edits().get(0).newText());
    }

    @Test
    void severityMappingCoversEveryLspSeverity() {
        assertEquals(DiagnosticSeverity.ERROR, JdtLsProjectDiagnostics.severity(org.eclipse.lsp4j.DiagnosticSeverity.Error));
        assertEquals(DiagnosticSeverity.WARNING, JdtLsProjectDiagnostics.severity(org.eclipse.lsp4j.DiagnosticSeverity.Warning));
        assertEquals(DiagnosticSeverity.INFO, JdtLsProjectDiagnostics.severity(org.eclipse.lsp4j.DiagnosticSeverity.Information));
        assertEquals(DiagnosticSeverity.HINT, JdtLsProjectDiagnostics.severity(org.eclipse.lsp4j.DiagnosticSeverity.Hint));
        assertEquals(DiagnosticSeverity.HINT, JdtLsProjectDiagnostics.severity(null));
    }

    @Test
    void conversionShiftsZeroBasedRangesToCoreOneBasedPositions() {
        Diagnostic converted = JdtLsProjectDiagnostics.toDiagnostic(new org.eclipse.lsp4j.Diagnostic(
                new Range(new Position(0, 4), new Position(0, 14)),
                "Foo cannot be resolved to a type", org.eclipse.lsp4j.DiagnosticSeverity.Error, "Java", "67108964"));

        assertEquals(DiagnosticSeverity.ERROR, converted.severity());
        assertEquals("67108964", converted.code());
        assertEquals("Foo cannot be resolved to a type", converted.message());
        assertEquals(1, converted.startLine());
        assertEquals(5, converted.startColumn());
        assertEquals(1, converted.endLine());
        assertEquals(15, converted.endColumn());
        assertEquals("Java", converted.category());
    }

    @Test
    void conversionDefaultsNullRangeNullSeverityAndIntegerCodes() {
        org.eclipse.lsp4j.Diagnostic raw = new org.eclipse.lsp4j.Diagnostic();
        raw.setMessage("problema");
        raw.setCode(Integer.valueOf(42));

        Diagnostic converted = JdtLsProjectDiagnostics.toDiagnostic(raw);

        assertEquals(DiagnosticSeverity.HINT, converted.severity());
        assertEquals("42", converted.code());
        assertEquals("problema", converted.message());
        assertEquals(1, converted.startLine());
        assertEquals(1, converted.startColumn());
        assertEquals(1, converted.endLine());
        assertEquals(2, converted.endColumn());
        assertEquals("jdt", converted.category());
    }

    @Test
    void unknownUriPublishIsDiscardedBeforeAnyListenerNotification() {
        JdtLsProjectDiagnostics holder = new JdtLsProjectDiagnostics(new JdtLsDocumentSync());
        List<List<Diagnostic>> received = new ArrayList<>();
        holder.setListener((uri, diagnostics) -> received.add(diagnostics));

        holder.onServerPublish(URI, List.of(rawError()));

        assertTrue(received.isEmpty());
    }

    @Test
    void publishWithUnknownSyncVersionIsDiscarded() {
        JdtLsProjectDiagnostics holder = new JdtLsProjectDiagnostics(new JdtLsDocumentSync());
        List<List<Diagnostic>> received = new ArrayList<>();
        holder.setListener((uri, diagnostics) -> received.add(diagnostics));

        holder.publishSynced(URI, List.of(rawError()), -1);

        assertTrue(received.isEmpty());
    }

    @Test
    void publishSyncedNotifiesTheListenerWithConvertedDiagnostics() {
        JdtLsProjectDiagnostics holder = new JdtLsProjectDiagnostics(new JdtLsDocumentSync());
        List<String> uris = new ArrayList<>();
        List<List<Diagnostic>> received = new ArrayList<>();
        holder.setListener((uri, diagnostics) -> {
            uris.add(uri);
            received.add(diagnostics);
        });

        holder.publishSynced(URI, List.of(rawError(), rawWarning()), 3);

        assertEquals(List.of(URI), uris);
        assertEquals(1, received.size());
        List<Diagnostic> converted = received.getFirst();
        assertEquals(2, converted.size());
        assertEquals(DiagnosticSeverity.ERROR, converted.get(0).severity());
        assertEquals(DiagnosticSeverity.WARNING, converted.get(1).severity());
        assertEquals("Java", converted.get(0).category());
    }

    @Test
    void listenerFailureNeverPropagatesToTheCaller() {
        JdtLsProjectDiagnostics holder = new JdtLsProjectDiagnostics(new JdtLsDocumentSync());
        holder.setListener((uri, diagnostics) -> {
            throw new IllegalStateException("boom");
        });

        holder.publishSynced(URI, List.of(rawError()), 1);
    }

    @Test
    void conversionIsClampedAgainstTheSyncedDocumentViaLineMap() {
        Diagnostic beyond = new Diagnostic(DiagnosticSeverity.ERROR, "", "boom", 50, 1, 50, 6, "jdt");

        List<Diagnostic> clamped = JdtLsProjectDiagnostics.clamp(List.of(beyond), "class Main {}");

        assertEquals(1, clamped.getFirst().startLine());
        assertEquals(1, clamped.getFirst().endLine());
    }

    @Test
    void quickFixMappingFromCodeActionWorkspaceEdits() {
        CodeAction action = new CodeAction("Criar classe 'Foo'");
        action.setKind("quickfix");
        action.setEdit(new WorkspaceEdit(Map.of(URI, List.of(
                new TextEdit(new Range(new Position(0, 4), new Position(0, 14)), "Foo")))));

        List<QuickFix> fixes = JdtLsProjectDiagnostics.toQuickFixes(URI, List.of(Either.<Command, CodeAction>forRight(action)));

        assertEquals(1, fixes.size());
        QuickFix fix = fixes.getFirst();
        assertEquals("Criar classe 'Foo'", fix.title());
        assertEquals("quickfix", fix.kind());
        assertEquals(1, fix.edits().size());
        assertEquals(1, fix.edits().getFirst().startLine());
        assertEquals(5, fix.edits().getFirst().startColumn());
        assertEquals(1, fix.edits().getFirst().endLine());
        assertEquals(15, fix.edits().getFirst().endColumn());
        assertEquals("Foo", fix.edits().getFirst().newText());
    }

    @Test
    void quickFixMappingSkipsNonQuickFixKindsAndForeignDocumentEdits() {
        CodeAction organizeImports = new CodeAction("Organizar imports");
        organizeImports.setKind("source.organizeImports");
        organizeImports.setEdit(new WorkspaceEdit(Map.of(URI, List.of(
                new TextEdit(new Range(new Position(0, 0), new Position(0, 1)), "x")))));
        CodeAction foreignEdit = new CodeAction("Corrigir em outro arquivo");
        foreignEdit.setKind("quickfix");
        foreignEdit.setEdit(new WorkspaceEdit(Map.of("file:///other/Other.java", List.of(
                new TextEdit(new Range(new Position(0, 0), new Position(0, 1)), "x")))));

        List<QuickFix> fixes = JdtLsProjectDiagnostics.toQuickFixes(URI,
                List.of(Either.<Command, CodeAction>forRight(organizeImports), Either.<Command, CodeAction>forRight(foreignEdit)));

        assertTrue(fixes.isEmpty());
    }

    @Test
    void quickFixMappingExtractsEditsFromApplyWorkspaceEditCommandArguments() {
        Map<String, Object> range = new LinkedHashMap<>();
        range.put("line", 0);
        range.put("character", 4);
        Map<String, Object> end = new LinkedHashMap<>();
        end.put("line", 0);
        end.put("character", 14);
        Map<String, Object> span = new LinkedHashMap<>();
        span.put("start", range);
        span.put("end", end);
        Map<String, Object> textEdit = new LinkedHashMap<>();
        textEdit.put("range", span);
        textEdit.put("newText", "Foo");
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put(URI, List.of(textEdit));
        Map<String, Object> edit = new LinkedHashMap<>();
        edit.put("changes", changes);
        Command command = new Command("Criar classe 'Foo'", "java.apply.workspaceEdit", List.of(edit));

        List<QuickFix> fixes = JdtLsProjectDiagnostics.toQuickFixes(URI, List.of(Either.<Command, CodeAction>forLeft(command)));

        assertEquals(1, fixes.size());
        QuickFix fix = fixes.getFirst();
        assertEquals("Criar classe 'Foo'", fix.title());
        assertEquals("quickfix", fix.kind());
        assertEquals("Foo", fix.edits().getFirst().newText());
        assertEquals(5, fix.edits().getFirst().startColumn());
    }

    @Test
    void quickFixMappingSkipsCommandsWithoutApplicableWorkspaceEdits() {
        Command withoutArguments = new Command("Criar classe 'Foo'", "java.apply.workspaceEdit");
        Command unrelatedCommand = new Command("Compilar", "java.compile", List.of());
        Command withMalformedArguments = new Command("Criar classe 'Foo'", "java.apply.workspaceEdit",
                List.of("not-a-workspace-edit"));

        List<QuickFix> fixes = JdtLsProjectDiagnostics.toQuickFixes(URI, List.of(
                Either.<Command, CodeAction>forLeft(withoutArguments), Either.<Command, CodeAction>forLeft(unrelatedCommand),
                Either.<Command, CodeAction>forLeft(withMalformedArguments)));

        assertTrue(fixes.isEmpty());
    }

    @Test
    void codeActionIsUnsupportedBeforeInitialization() throws Exception {
        JdtLsSession session = new JdtLsSession(configuration(
                temporaryDirectory.resolve("workspace"), temporaryDirectory.resolve("data")));

        assertFalse(session.supportsCodeAction());
        session.close();
    }

    @Test
    void sessionStoresPublishedDiagnosticsPerUriAndNotifiesTheListener() throws Exception {
        JdtLsSession session = new JdtLsSession(configuration(
                temporaryDirectory.resolve("workspace"), temporaryDirectory.resolve("data")));
        List<String> uris = new ArrayList<>();
        List<List<org.eclipse.lsp4j.Diagnostic>> received = new ArrayList<>();
        session.setDiagnosticsListener((uri, diagnostics) -> {
            uris.add(uri);
            received.add(diagnostics);
        });

        client(session).publishDiagnostics(new PublishDiagnosticsParams(URI, List.of(rawError())));

        assertEquals(List.of(URI), uris);
        assertEquals(1, received.size());
        assertEquals(1, received.getFirst().size());
        assertEquals(1, session.publishedDiagnostics(URI).size());
        assertTrue(session.publishedDiagnostics("file:///other/Other.java").isEmpty());
        session.close();
    }

    @Test
    void blankPublishParamsAreIgnored() throws Exception {
        JdtLsSession session = new JdtLsSession(configuration(
                temporaryDirectory.resolve("workspace"), temporaryDirectory.resolve("data")));
        List<List<org.eclipse.lsp4j.Diagnostic>> received = new ArrayList<>();
        session.setDiagnosticsListener((uri, diagnostics) -> received.add(diagnostics));

        client(session).publishDiagnostics(null);
        client(session).publishDiagnostics(new PublishDiagnosticsParams("", List.of(rawError())));

        assertTrue(received.isEmpty());
        session.close();
    }

    @Test
    void attachWiresTheHolderIntoTheSessionPublishAndDetachUnregistersIt() throws Exception {
        JdtLsSession session = new JdtLsSession(configuration(
                temporaryDirectory.resolve("workspace"), temporaryDirectory.resolve("data")));
        JdtLsProjectDiagnostics holder = new JdtLsProjectDiagnostics(new JdtLsDocumentSync());
        List<List<Diagnostic>> received = new ArrayList<>();
        holder.setListener((uri, diagnostics) -> received.add(diagnostics));

        holder.attach(session);
        client(session).publishDiagnostics(new PublishDiagnosticsParams(URI, List.of(rawError())));
        assertTrue(received.isEmpty());

        holder.detach();
        client(session).publishDiagnostics(new PublishDiagnosticsParams(URI, List.of(rawError())));
        assertTrue(received.isEmpty());
        session.close();
    }

    private static org.eclipse.lsp4j.Diagnostic rawError() {
        return new org.eclipse.lsp4j.Diagnostic(new Range(new Position(0, 4), new Position(0, 14)),
                "Foo cannot be resolved to a type", org.eclipse.lsp4j.DiagnosticSeverity.Error, "Java");
    }

    private static org.eclipse.lsp4j.Diagnostic rawWarning() {
        return new org.eclipse.lsp4j.Diagnostic(new Range(new Position(2, 0), new Position(2, 9)),
                "aviso", org.eclipse.lsp4j.DiagnosticSeverity.Warning, "Java");
    }

    private static LanguageClient client(JdtLsSession session) throws Exception {
        Field field = JdtLsSession.class.getDeclaredField("client");
        field.setAccessible(true);
        return (LanguageClient) field.get(session);
    }

    private JdtLsProcessConfiguration configuration(Path workspace, Path data) throws Exception {
        Path installation = Files.createDirectories(temporaryDirectory.resolve("jdtls"));
        Path plugins = Files.createDirectories(installation.resolve("plugins"));
        Files.createFile(plugins.resolve("org.eclipse.equinox.launcher_1.jar"));
        String os = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        Files.createDirectories(installation.resolve(os.contains("win") ? "config_win"
                : os.contains("mac") ? "config_mac" : "config_linux"));
        return JdtLsProcessConfiguration.fromInstallation(JdtLsToolingJava.currentRuntime(Duration.ofSeconds(5)),
                installation, data, Files.createDirectories(workspace));
    }
}
