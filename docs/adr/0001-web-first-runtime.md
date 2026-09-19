# ADR 0001: Web-first runtime

## Context

EyeCode historically included Swing/JCEF and JavaFX/CEFFX launch paths. The
primary development/runtime command now starts a local HTTP and WebSocket Web
Shell, while React and Monaco provide the main user interface. Removing the
desktop adapters is not currently safe: they retain entry points, native
integration paths and tests.

## Decision

`LocalWebShellLauncher` is the primary runtime entrypoint. It starts
`LocalWebShellRuntime`, which serves the React bundle and transports the
stable internal `eyecode.web/1` envelopes over loopback WebSocket.

JavaFX/CEFFX and Swing/JCEF remain legacy-but-required adapters. They use the
same Web Shell contracts and composition, but the primary Web runtime does not
depend on their toolkits.

## Consequences

New IDE behavior belongs in Core/Application first, then in the shared Web
Shell contract and React feature when it needs UI. Desktop-specific behavior is
kept inside its adapter. A future frontend/transport replacement can preserve
the application services and protocol semantics while replacing the outer
adapter.

Desktop code is retained until entrypoint, native loading and manual diagnostic
uses have explicit removal evidence.

## Status

Accepted.
