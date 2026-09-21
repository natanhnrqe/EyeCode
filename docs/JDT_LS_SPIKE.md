# JDT LS Spike 1

This spike proves a backend-only, external Eclipse JDT Language Server connection. Production EyeCode completion does not use JDT LS yet.

## Pinned fixture

- JDT LS: `1.61.0-202609031315`
- Archive: `https://download.eclipse.org/jdtls/milestones/1.61.0/jdt-language-server-1.61.0-202609031315.tar.gz`
- SHA-256: `338e7e73d61836651ba2453919a0d34fa763eb4e7c03342092309bffb8934c64`
- Tooling Java: Java 21 or newer

The fixture is a development cache, not repository source and not a release artifact. Download it once, verify the published SHA-256, and extract it outside the repository.

## Running the real proof

```powershell
mvn "-Dtest=JdtLsSemanticCompletionIntegrationTest" `
  "-Deyecode.jdtls.integration=true" `
  "-Deyecode.jdtls.home=C:\path\to\jdt-language-server-1.61.0-202609031315" test
```

Without `eyecode.jdtls.integration=true`, the real external-process test is skipped. Ordinary unit tests do not download JDT LS and do not require a local JDT LS installation.

## Boundary and lifecycle

`com.eyecode.language.java.lsp` owns the pinned-process configuration, Java tooling verification, stdio LSP connection and lifecycle. It does not integrate with Monaco, React, WebShell, Learn, project lifecycle, run, terminal, current completion or diagnostics services.

The session owns daemon LSP and stderr executors, captures a bounded stderr tail, and follows:

```text
NEW → STARTING → INITIALIZING → READY → SHUTTING_DOWN → STOPPED
                         └──────────────────────────→ FAILED
```

The test creates separate temporary directories:

```text
temporary/
├── workspace/
│   └── src/Main.java
└── jdt-data/
```

`-data` is never the user project, repository or Learn workspace. The proof opens `Main.java` with a `file://` URI and requests completion after `value.sub`; JDT LS must return a String member whose label begins with `substring(`. LSP positions use the default UTF-16, zero-based line and character convention.

No Maven or Gradle project is imported, no wrappers run, no dependency download is initiated by the test, and no annotation processor or Lombok agent is enabled.

## Known limits

Phase 1 enables project-only completion when `-Deyecode.jdtls.home=<pinned-installation>` is explicitly configured. The Web Shell composition owns one asynchronous session for the active project. While JDT is unavailable, starting, initializing, stopped, or fails, the existing `JavaCompletionProvider` remains the fallback.

Only real Java files under the active project root are eligible. `lesson://`, untitled, and non-project documents never reach JDT LS. `EditorDocument` remains the text/version authority: its source is projected through `didOpen` once, full-text `didChange` on later versions, and `didClose` when the editor session closes. JDT `-data` remains separate at `eyecode-jdtls/<workspace-hash>` under the system temporary directory.

JDT completion items are mapped into the existing neutral `CompletionCandidate` model. A valid empty JDT result stays empty; failures and timeouts use the existing local completion. Completion resolve, text edits, additional edits, diagnostics, hover, navigation, formatting, code actions, refactoring, semantic tokens, and Learn integration remain deferred.
