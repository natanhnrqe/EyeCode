# ADR 0003 — Language identity and optional capabilities

## Status

Accepted

## Context

The Web Shell previously selected Java completion and diagnostics directly in its controllers. A document's language was inferred from Java-oriented defaults, so a URI without a filesystem extension, including `lesson://`, had no durable language identity. `EditorManager` also named Java lexer and semantic implementations directly.

EyeCode currently teaches and analyzes Java, but language support must not force application consumers, protocol controllers, or Monaco DTOs to know Java implementation classes.

## Decision

Document language is represented by `LanguageId` and `LanguageDocument`. A declared language takes precedence; otherwise `ExtensionDocumentLanguageResolver` resolves a registered extension. Unknown documents have no optional language capability and receive an empty supported result.

Completion and diagnostics are independent capabilities. `CompletionProvider`/`CompletionService` and `DiagnosticsProvider`/`DiagnosticsService` route by resolved `LanguageId`. Java is supplied by explicit adapters in composition: `JavaCompletionProvider` and `JavaDiagnosticsProvider`.

`EditorManager` depends on the focused `EditorIntelligence` contract for document lifecycle and definition lookup. `JavaEditorIntelligence` is injected explicitly by the Web composition and owns the Java lexer/event bridge and semantic implementation.

Web controllers adapt protocol payloads to neutral requests and serialize neutral results. Monaco sends each model's declared language with completion and diagnostics requests. Controllers do not instantiate Java analyzers, Java completion providers, or Monaco completion DTOs.

## Consequences

A future language changes composition by registering its identity/extensions and only the providers it implements. It does not require a new Web controller, protocol channel, service locator, reflection, ServiceLoader, or plugin framework.

Lexer/parser/AST and smart-edit rules remain deliberately Java-specific where only Java has a real implementation or consumer. Generalizing them is deferred until a second implementation requires a shared contract.

Language work remains synchronous inside the capability. The Web controllers retain their bounded single-worker execution and request-id stale-result suppression; provider results are not cached globally. Java's existing document cache remains lifecycle-bound through `JavaEditorIntelligence`.
