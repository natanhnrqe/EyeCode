# ADR 0004: Contain legacy desktop behind compatibility boundaries

## Status

Accepted.

## Context

EyeCode's primary runtime is the local Web Shell. Swing/JCEF and JavaFX/CEFFX still compile and remain required compatibility adapters, with independent entrypoints and native lifecycle concerns. The project must prevent active Core, Application, runtime and Web contracts from acquiring dependencies on either desktop generation.

## Decision

Desktop implementations consume shared capabilities and Web Shell contracts through explicit existing seams such as `WebShellSurface`, `WebShellNativeUi`, `WebShellNativeFileSelection`, `WebShellWindowControls`, `EditorViewFactory` and `FileSystemService`. The Web composition remains independent of desktop implementations. No universal UI framework, provider registry or toolkit factory is introduced.

Architecture tests protect active service packages and active Web contracts/composition from Swing, JavaFX, JCEF and CEFFX references. Desktop roots remain independently composed: `SwingApplication`/`SwingMainWindow` and `FxApplication`/`FxMainWindow`.

## Consequences

New shared behavior is implemented outside desktop legacy packages and adapted by the relevant desktop surface. Swing and JavaFX are not scheduled for removal by this decision. A future deletion requires independent evidence of no production consumer, documented or manual entrypoint, reflective/configuration use, and relevant test coverage.
