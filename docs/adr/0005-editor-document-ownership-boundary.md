# ADR 0005: Keep mutable document and command execution under editor v2

## Status

Accepted.

## Context

`EditorDocument` owns mutable text, versioning, dirty state and mutation batches. `EditorBuffer` owns interaction state and `CommandManager` owns undo/redo. The immutable document values used by language intelligence lived in `editor.intelligence.document`, but `DocumentTransaction` was also placed there despite directly executing `editor.v2` commands against `EditorDocument`. That created a dependency cycle between immutable document values and the concrete editor model.

## Decision

Keep immutable snapshot, range, line-map, change and event values in `editor.intelligence.document` and events. Move `DocumentTransaction` beside `CommandManager` in `editor.v2.command`. The transaction remains an applied-edit executor with batching and grouped undo semantics. Smart-edit interaction code may use that concrete command transaction; language intelligence continues to consume snapshots and document-change events only.

No new document port, repository, provider, adapter or factory is introduced. `EditorManager` remains the lifecycle owner of sessions, documents, buffers, views, autosave and external-file watching.

## Consequences

The immutable document-value package no longer depends on `editor.v2`, removing the structural document-model cycle. The behavior and public transaction API are preserved at their owned package. Caret and selection remain interaction/session state rather than document state. A source-level architecture test protects the dependency direction.
