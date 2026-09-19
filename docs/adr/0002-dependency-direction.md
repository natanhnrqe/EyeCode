# ADR 0002: Composition and dependency direction

## Context

The former Web workspace controller constructed editor, filesystem, project,
run and terminal services while also translating Web protocol messages. Native
desktop UI exposed one broad capability to controllers that only required a
file picker or window controls. This mixed composition, application lifecycle,
protocol adaptation and toolkit knowledge at the boundary.

## Decision

`WorkspaceApplication` owns the shared workspace service lifecycle and exposes
no factory or service getters. `WebShellWorkspaceComposition` is the Web
composition root: it visibly selects `DefaultFileSystemService`, one `EventBus`,
the shared `ProjectFileOperationService`, editor/project/run/terminal services,
Maven project creation and every Web controller. `WebShellWorkspaceRuntime`
owns that controller set and closes it before the application services.

Web protocol responsibilities are separated by cohesive channel cluster:
`WebShellWorkspaceController` owns workspace/document-related flows and
`WebShellExecutionController` owns `run` and `terminal`; both receive their
explicit service dependencies rather than an application context. Completion,
learning, lessons and diagnostics are sibling controllers created by
composition. Native capabilities
are segregated into `WebShellNativeFileSelection` and
`WebShellWindowControls`; `WebShellNativeUi` remains a compatibility facade
implemented by desktop adapters.

## Consequences

Core/Application does not import Web or desktop adapters. Controllers no
longer create or look up the workspace services they use, and a file-selection
consumer does not depend on window controls. The EventBus is synchronous,
in-process and remains inside the document-to-lexer path; it is not bridged to
WebSocket. Tests enforce those dependency directions and the functional lexical
event flow.

This is constructor injection only; no DI framework or global container is
introduced. Maven execution and terminal WebSocket transport remain concrete
infrastructure until a second real implementation creates a consumer-driven
port.

## Status

Accepted.
