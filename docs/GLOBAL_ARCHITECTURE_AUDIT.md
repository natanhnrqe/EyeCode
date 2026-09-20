# Global Architecture Audit

Audit date: 2026-09-19. Scope: production source under `com.eyecode`, React source under `src/main/web`, architecture tests, runtime registration, and repository documentation. This is an inspection report; it intentionally makes no source, package, legacy-removal, or abstraction change.

## Executive conclusion

The active product path is Web-first and its dependency direction is substantially healthy:

`LocalWebShellLauncher -> LocalWebShellRuntime -> WebShellWorkspaceComposition -> application/workbench/project/runtime/terminal -> WebShell protocol -> React/Monaco`.

The main architectural pressure is not a Maven, Web, language, or Presentation leak. The former desktop-containment, document-cycle and terminal-package findings have now been addressed at their proven boundaries. Remaining desktop generations are contained legacy rather than an active-path refactoring frontier.

## 1. Production package map

The table groups every production package family; an asterisk includes its listed subpackages.

| Package family | Purpose and principal classes | Layer | State |
| --- | --- | --- | --- |
| root (`Main`) | Historical Swing entrypoint | DESKTOP_LEGACY | Legacy required for compatibility |
| `application` | Workspace orchestration: `WorkspaceApplication`, `WorkspaceProjects`, `ProjectExplorerQuery` | APPLICATION | Active |
| `autosave` | Debounced persistence/external-change coordination: `AutoSaveManager` | INFRASTRUCTURE | Active |
| `browser`, `diag`, `spike` | JCEF singleton, diagnostic validator, Swing Web Shell experiment | DESKTOP_LEGACY / UTILITY | Mixed; see dead-code evidence |
| `command` | Historical command/event-bus actions | DESKTOP_LEGACY | Legacy required |
| `designsystem.icon` | Toolkit-neutral icon identifiers | UTILITY | Active shared |
| `diagnostics` | Java compiler diagnostic adapter and DTOs | LANGUAGE_IMPLEMENTATION | Active |
| `editor` | Historical editor context/tab model | DESKTOP_LEGACY | Mixed |
| `editor.intelligence.*` | Snapshot, transactions, caret, indent, selection and smart-edit pipeline | DOMAIN | Active core |
| `editor.v2`, `editor.v2.command`, `completion.*`, `syntax.*`, `language.*`, `ui.*` | Editor buffer/model, Java-specific completion/parser/style, Rich Swing view | MIXED | Core model active; Swing UI legacy |
| `eventbus`, `eventbus.events` | Synchronous application-scoped typed bus | INFRASTRUCTURE | Active shared |
| `explorer.*` | Historical explorer model/builder/visitor/Swing UI | DESKTOP_LEGACY | Mixed |
| `filesystem` | File port, NIO implementation and watcher | PORT / INFRASTRUCTURE | Active |
| `javafx.*`, `javafx.ceffx`, `javafx.editor`, `explorer`, `learning`, `monaco`, `ui.*`, `designsystem` | JavaFX/CEFFX desktop surface and Monaco adapter | DESKTOP_LEGACY | Legacy required |
| `language`, `ast`, `cfg.*`, `completion`, `diagnostics`, `documentation`, `java.*`, `semantic`, `symbol` | Language identity/capabilities and Java lexer/parser/semantic implementation | DOMAIN / LANGUAGE_IMPLEMENTATION | Active |
| `learning.*`, `catalog`, `content`, `curriculum`, `hover`, `html`, `markdown.*`, `model`, `renderer`, `service`, `swing`, `ui`, `web` | Earlier Learn/Markdown pipeline and desktop renderers | MIXED | Legacy and shared content; not active Web lesson presentation |
| `lessons.catalog`, `content`, `practice`, `presentation`, `session` | Resource catalog, lesson state, verification, presentation compiler | DOMAIN | Active |
| `maven` | POM parsing model | INFRASTRUCTURE | Active support |
| `project`, `model`, `template`, `wizard.*` | Project lifecycle/model, explicit Maven creation, templates and historical wizard | APPLICATION / MIXED | Active lifecycle; desktop wizard legacy |
| `run` | Older direct Java/Maven runner stack | DESKTOP_LEGACY | Legacy candidate, not active Web runtime |
| `runtime` | Current run lifecycle, configuration, resolution, process session | APPLICATION / INFRASTRUCTURE | Active; semantically mixed |
| `swing`, `ui`, `ui.core`, `ui.editor`, `ui.scroll`, `ui.swing` | Swing desktop windows, components and design system | DESKTOP_LEGACY | Legacy required |
| `terminal`, `terminal.swing` | Web terminal service/session/transport and isolated Swing/JediTerm presentation | INFRASTRUCTURE / DESKTOP_LEGACY | Active runtime and contained legacy UI |
| `ui.designsystem` | Shared color/typography/spacing/icon services | UTILITY | Active shared |
| `ui.web`, `ui.web.learning`, `ui.web.monaco` | Envelope/dispatcher, HTTP/WebSocket and native surfaces, Web controllers, Monaco payloads | WEB_UI / ADAPTER / COMPOSITION | Active |
| `workbench.editor`, `workbench.toolwindow` | Editor session lifecycle, autosave integration, workbench state | APPLICATION | Active |

## 2. Frontend map

`src/main/web/src` contains 37 TypeScript/TSX modules. `bridge` owns `EyeCodeBridge`, WebSocket/native transport and protocol types. `workspace` is the primary React composition/state surface (17 modules) and renders project tree, documents, run and terminal state. `monaco` owns browser-side models, presentation playback and bridge adaptation. `lessons` owns catalog/player UI, `learning` owns overlay behavior, `completion` and `diagnostics` render neutral results, and `document` holds document-facing types. There is no Maven, filesystem, Java process, or native UI implementation in the frontend.

## 3. Relevant dependency graph

```text
ui.web composition -> application/workbench/project/runtime/terminal/filesystem/language/lessons
ui.web controllers -> explicit application services and WebShellSurface
workbench.editor -> editor.v2 + autosave + filesystem + project + EventBus
editor.intelligence -> document contracts; transaction bridge -> editor.v2 command/model
language.java -> lexer/parser/semantic/symbol + EventBus bridge
lessons.session -> lessons.content -> lessons.presentation/practice
runtime -> project.model + Maven/JDK/process infrastructure
terminal -> Pty4J + terminal WebSocket transport
React/Monaco <-> eyecode.web/1 <-> ui.web
JavaFX/Swing legacy -> shared contracts, WebShell surfaces and older UI/editor packages
```

No active `application -> ui.web`, core -> desktop toolkit, Web controller -> desktop adapter, or active Web -> Maven direct dependency was found. `ArchitectureBoundaryTest` checks these major directions in source and bytecode.

## 4. Cycles

Static import analysis found package-level mutual references. Most are intra-feature implementation cycles, not runtime cycles.

| Cycle | Classification | Evidence |
| --- | --- | --- |
| `editor.intelligence.document <-> editor.v2` | RESOLVED | Immutable document values remain in `editor.intelligence.document`; `DocumentTransaction` moved beside `CommandManager` in `editor.v2.command`, removing the reverse dependency. |
| `language.semantic <-> language.symbol` and `language.java <-> semantic/symbol` | BENIGN / review on next language change | Resolver and symbol model collaborate bidirectionally inside one language implementation. |
| `language.java <-> workbench.editor` | BENIGN bridge | Event bridges connect document changes to lexer/parser updates; composition owns lifecycle. |
| `eventbus.events <-> workbench.editor/toolwindow` | STRUCTURAL_DEBT | Some event DTOs carry workbench types, which makes generic event packages less neutral. |
| `terminal.swing <-> ui` | LEGACY_ONLY | The isolated Swing terminal presentation consumes historical Swing UI support; active terminal runtime has no reverse dependency. |
| `ui <-> ui.editor`, `editor.v2 <-> v2.*` | BENIGN legacy implementation cycles | Historical Swing composition and editor implementation. |
| `lessons.catalog <-> content`, `learning.content <-> model/html` | BENIGN feature cohesion | Content parsing/model APIs are mutually referenced. |

## 5. Top fan-out (architectural imports)

| Class | Imports / LOC | Assessment |
| --- | ---: | --- |
| `RichEditorView` | 69 / 1,862 | Legacy Swing aggregate; high fan-out is expected but it is a major containment target. |
| `FxEditorWorkspacePane` | 39 / 698 | JavaFX legacy composition/view. |
| `JavaFxEditorController` | 39 / 546 | JavaFX editor adapter. |
| `MainWindow` | 25 / 889 | Historical Swing composition. |
| `WebShellLearningController` | 23 / 377 | Cohesive Web learning adapter, but a future review point. |
| `SymbolTableBuilder` | 22 / 759 | Language implementation with broad AST/symbol inputs. |
| `WebShellWorkspaceComposition` | 20 / 90 | Legitimate composition root. |
| `JavaCompletionProvider` | 20 / 75 | Java capability adapter. |
| `WebShellLessonsController` | 18 / 271 | Cohesive session/protocol adapter. |
| `EditorManager` | 17 / 515 | Active lifecycle aggregate. |
| `FxRootLayout` | 16 / 404 | JavaFX legacy composition. |
| `SemanticCompletionProvider` | 16 / 303 | Java-specific semantic integration. |
| `LearningHoverController` | 15 / 568 | Older Learn aggregate. |
| `WebShellDocumentController` | 14 / 467 | Active document protocol adapter. |
| `JavaTypeMemberResolver` | 13 / 348 | Language implementation. |
| `EditorBuffer` | 12 / 198 | Core editor model bridge. |
| `ProjectCompletionProvider` | 11 / 130 | Completion adapter. |
| `SmartEnterStrategy` | 11 / 142 | Core editing strategy. |
| `JavaFxLearningWorkspace` | 19 / 205 | Legacy JavaFX composition. |
| `MonacoLearningCardRenderer` | 16 / 268 | JavaFX/CEFFX legacy renderer. |

## 6. Top fan-in

The highest-consumed boundaries are `TextRange` (58 importers), `DocumentSnapshot` (44), `ColorManager` (38), `LearningConcept` (33), `EditorDocument` and `CompletionItem` (32 each), `TypographyManager` (27), `ProjectModel` (26), `EditorCommandContext` (25), `EditorBuffer` (24), and `EventBus`/`Token` (20). These are genuine blast-radius types; changes should be additive and validated broadly.

## 7. Large classes

`JavaParser` (2,173 LOC) and completion databases (up to 1,294 LOC) are data/parser-heavy and should not be split only for length. `RichEditorView` (1,862), `EditorPanel` (1,448), `MainWindow` (889), `SymbolTableBuilder` (759), `FxEditorWorkspacePane` (698), `LearningHoverController` (568), `JavaFxEditorController` (546), `EditorManager` (515), and `WebShellDocumentController` (467) deserve review by responsibility. The Swing/JavaFX view classes are **STRUCTURAL_DEBT only within legacy containment**; `EditorManager`, `SymbolTableBuilder`, and Web controllers are **REVIEW**, not automatic refactor candidates.

## 8. Constructor analysis

High dependency counts are mostly legitimate orchestration: `WebShellWorkspaceComposition` wires concrete services; `EditorManager` owns editor/autosave/watcher lifecycle; Web controllers receive explicit collaborators; `WorkspaceApplication` is lifecycle-only. No controller obtains services from `WorkspaceApplication`. `MainWindow`, JavaFX roots, and legacy editors construct more directly and are the meaningful legacy concentration rather than a reason to introduce a service locator.

## 9. Static and global state

Most static state is immutable constants, patterns, catalogs or enums. Mutable/global uses are limited and classified as follows: `BrowserManager` singleton and `LearningChromiumView` shared `CefApp` are **legacy native-runtime state**; `RichEditorView.SKIP_HOVER`, `CaretSynchronizationManager.SYNC_ENABLED`, Swing hover registration flags, and `LearningChromiumCard.USE_CEF` are **legacy UI toggles**; `SymbolScopeImpl` ID generator is **safe process-local identity**; system properties are **launch configuration**, not application service state. No global EventBus was found.

## 10. Threads and executors

| Owner | Thread/executor | Shutdown | Daemon / cancellation |
| --- | --- | --- | --- |
| `AutoSaveManager` | single scheduled `AutoSaveManager` | `dispose` | daemon; scheduled tasks cancelled |
| `ExternalFileWatcher` | single watcher | `close` | daemon; WatchService closed |
| `RunService` | single preparation executor | `dispose` | daemon; future interrupted |
| `RunSession` | cached stream executor + scheduled force-stop | completion/dispose | daemon; process tree graceful then forced |
| `TerminalSession` | single session executor + scheduled force-stop | completion/dispose | daemon; Pty process stop |
| `WebShellAssetServer` | cached HTTP executor | `close` | daemon; server stopped |
| `WebShellCompletionController`, `WebShellLearningController`, diagnostics | single worker each | `dispose` | daemon; cancellation/close |
| JavaFX/Swing legacy views | JavaFX futures, UI-specific worker threads | adapter disposal | legacy lifecycle |

No active Web-owned executor was found without a close/dispose owner. Cached pools are unbounded by design and remain an operational risk rather than a current leak finding.

## 11. EventBus map

`EventBus` is application-scoped, exact-type and synchronous. `EditorBuffer` publishes `DocumentTextChangeEvent`; `LexerEventBridge` and `ParserEventBridge` subscribe and publish `TokensUpdatedEvent` and `ParserSnapshotUpdatedEvent`. `EditorManager` publishes editor/file/project refresh events. Historical command classes publish file/project events for Swing flows. Subscribers run on the publisher thread and bridge subscriptions hold `SubscriptionToken`s released on disposal. The bus remains small and is not a temporal service locator, though event DTOs that mention workbench types are minor structural debt.

## 12. Resource ownership

| Resource | Created / owned by | Closed by |
| --- | --- | --- |
| HTTP/WebSocket Web Shell | `LocalWebShellRuntime` / `LocalWebShellSurface`, `WebShellAssetServer` | runtime `close` |
| terminal WebSocket and PTY | `TerminalService` | `stop`/`dispose` |
| project run process | `RunSession` | session completion/stop/dispose |
| Maven classpath process | `MavenClasspathResolver` | resolver finally/interruption |
| file watch service | `ExternalFileWatcher` via `EditorManager` | manager close |
| autosave | `EditorManager` | manager close |
| EventBus bridges | `JavaEditorIntelligence` | intelligence close |
| browser opener | `LocalWebShellBrowserOpener` | no retained resource |

## 13–18. Package reviews and desktop inventory

`application` contains orchestration/lifecycle only; no Web, toolkit, Maven or transport import was found. `runtime` is active but mixes application lifecycle (`RunService`, configuration), resolution (`ProjectExecutionResolver`), Maven classpath infrastructure and process implementation (`RunSession`, `ProcessTree`); its semantics are imperfect but the previous run audit found no justified new abstraction. `command` is used by `MainWindow` and explorer integration, not the active Web runtime: **LEGACY_REQUIRED**. `diagnostics` is active Java diagnostics; `diag.JcefValidator` is a historical JCEF diagnostic utility with no active composition use. `terminal` keeps active `TerminalService`/`TerminalSession`/WebSocket transport in its runtime package and isolates the Swing JediTerm stack in `terminal.swing`.

JavaFX/CEFFX compiles, has tests, and has `FxApplication` as a desktop entrypoint, but is not called by the local Web runtime: **LEGACY_REQUIRED**. Swing/JCEF likewise compiles and adapts the shared Web Shell: **LEGACY_REQUIRED**. `BrowserManager` has no active Web composition registration but remains a **LEGACY_REQUIRED** JCEF runtime owner for `LearningBrowserService` and `LearningChromiumCard`. `JcefValidator` and `SwingWebShellSpike` remain **UNCERTAIN** diagnostic/manual legacy pending a separate reflective/configuration and manual-entrypoint review, not deletion. Shared `ui.designsystem` and Web protocol DTOs are **ACTIVE_SHARED**.

## 19–24. Feature maps

Lessons: resource catalog -> normalized content -> `LessonSessionService` -> practice verification and `PresentationCompiler`; Web controllers adapt session snapshots and payloads. Presentation is bounded as resource authoring -> parser/change analyzer -> planner -> `PresentationProgram`/operations -> JSON payload -> React player/Monaco. No Presentation source was changed.

Editor: `EditorManager` owns sessions, buffers, autosave and watcher; `EditorBuffer`/`EditorDocument` expose snapshots and events; intelligence and language react through bridges; Web document/completion/diagnostics controllers adapt them for Monaco. Project: Web workspace controller -> `WorkspaceProjects` -> lifecycle/model/file operations/explorer query -> runtime resolver/run service. Maven creation is explicit; execution resolution remains Java/Maven-specific below the application lifecycle.

Protocol `eyecode.web/1` channels are: `workspace` (snapshot/open/create/children/file operations plus changed/reset/treeChanged), `document` (open/new/activate/change/save/close/layout and lifecycle events), `completion/request`, `diagnostics/request` plus publish/failure, `run` (state/run/rerun/stop/selectConfiguration/output), `terminal` (show/hide/restart/resize/state/status/stop), `lessons` catalog/session actions, `learning` request/close/documentation/JDK source, `native` window actions, and `shell/bootstrap`. Naming is mostly feature-cohesive; `terminal state` and `terminal status` are compatibility aliases, not a new duplicate domain.

## 25–29. Duplication, dead code, tests, and docs

Frontend/backend duplication is largely a required presentation mirror: protocol DTOs mirror run state, document state and capabilities; frontend language IDs mirror backend IDs at the transport boundary. Path normalization, filesystem access, classpath resolution and lesson/presentation rules are backend-owned. No architectural duplication of Maven or Java parsing was found in TypeScript.

`spike.SwingWebShellSpike` and `diag.JcefValidator` have no active Web runtime registration, but their diagnostic/manual status remains uncertain and they are not removed by this audit. `BrowserManager` is not a deletion candidate because `LearningBrowserService` consumes it in production. The historical `run` package is **UNCERTAIN/legacy**, because it has no active Web composition path but retains tests and may support desktop workflows.

Tests are concentrated in Java unit/integration packages: language (74), editor (47), JavaFX (35), learning (32), UI (19), runtime/project/workbench/Web integration. Real temporary filesystem/process/Web tests are deliberate. Environment-sensitive clipboard tests remain isolated historical Swing behavior. Critical coverage includes `ArchitectureBoundaryTest`, Web asset/runtime/protocol tests, lifecycle tests, and real run/stop tests. Gaps: no frontend unit-test suite was found, and documentation rules around legacy package containment are only partially automated.

`docs/ARCHITECTURE.md`, `DEVELOPMENT_GUIDE.md`, and ADRs 0001–0005 match the active composition and the post-refactor ownership boundaries. The remaining `ui.web` native adapter implementations are legacy desktop adapters selected only by desktop roots; the local launcher composes unavailable native UI and remains toolkit-free.

## 30. Renewability matrix

| Change | Existing files likely modified | New files | Blast radius / boundary |
| --- | --- | --- | --- |
| Replace React | Web bridge/player only | frontend implementation | Low; `eyecode.web/1` |
| Replace WebSocket | local surface/transport | transport adapter | Low-medium; `WebShellSurface` |
| Replace Monaco | frontend Monaco service and DTO adapter | renderer | Medium; protocol remains |
| Add language Foo | resolver registration and selected capabilities | lexer/providers | Medium; language capability contracts |
| Replace Java lexer | `language.java` implementation | lexer implementation | Medium; `LexerService` |
| Add Gradle | detector/resolver/build executable paths | Gradle resolver | Medium; no RunService rewrite |
| Replace Maven | classpath/resolution implementation | resolver adapter | Medium; `RunService` stays |
| Add desktop frontend | composition/native surface | adapter | Medium; Web contracts reusable |
| Replace filesystem implementation | `FileSystemService` composition | implementation | Low-medium |
| Replace terminal backend | `TerminalService`/session transport | backend adapter | Medium |
| Add lesson type | lesson content/session/controller/player | type-specific service | Medium |
| Replace presentation player | frontend player/Monaco application | player | Low-medium; `PresentationProgram` |

## 31–33. SOLID, risks, and debt

Top findings: (1) S—legacy `RichEditorView` and desktop roots aggregate many UI concerns (**legacy debt**); (2) S/O—`runtime` combines resolver/process/Maven concepts (**minor debt**, no proven abstraction yet); (3) D—event DTOs carry workbench types (**minor debt**); (4) O—`ProjectExecutionResolver` would change for a real second build tool, accepted until that implementation exists; (5) static native UI toggles are legacy global state; (6) synchronous EventBus propagates subscriber failures, accepted by contract.

Top risks: unbounded cached HTTP/run stream pools (medium/low); synchronous EventBus failure propagation (medium/low); partial Gradle branches (medium/low); and the absence of a frontend unit-test suite (medium/medium). Desktop UI/JCEF size and static toggles remain legacy risks, not active Web architecture risks.

Debt register: **minor**—runtime semantic mix, event DTO coupling and process-local catalog mutability; **structural**—none that currently violate an active dependency direction; **legacy**—Swing/JavaFX editors, native adapters, command/run/explorer generations; **accepted**—Maven-specific resolver and Java-specific language implementations until a second real consumer exists.

## 34–36. Historical candidate frontiers and recommendation

1. **Desktop legacy containment** — completed; desktop adapters remain isolated behind existing seams.
2. **Editor model package boundary** — completed; immutable document values no longer depend on `editor.v2`.
3. **Terminal core versus Swing widget separation** — completed; Swing/JediTerm presentation is in `terminal.swing`.

The following post-refactor status supersedes the historical frontier recommendation.

## Post-refactor status

- Desktop containment is complete for the active Web composition: `LocalWebShellLauncher`, `LocalWebShellRuntime`, `LocalWebShellSurface`, controllers and composition do not bootstrap a desktop toolkit. Native Swing/JavaFX implementations remain compatibility adapters selected only by their desktop roots.
- The `editor.intelligence.document <-> editor.v2` cycle is removed. `EditorDocument` remains mutable text/version/dirty authority, `EditorBuffer` owns interaction/history state, immutable document values remain analytical views, and `DocumentTransaction` is owned beside the command executor.
- Terminal Swing presentation is separated in `terminal.swing`. The active `TerminalService`, `TerminalSession` and `TerminalWebSocketTransport` do not depend on that package or Swing/JediTerm.
- Architecture tests protect application, Web composition/controller, language neutrality, document-value and active-terminal boundaries. The remaining mutual references are either feature-internal or legacy-only.
- Current validated baseline: 2176 tests, 0 failures, 0 errors and 7 skipped; frontend typecheck and production build pass.
- **Structural freeze recommendation: recommended.** No remaining active-layer dependency violation, duplicate authority, critical active cycle, or unowned lifecycle/resource evidence justifies another architectural refactor before feature work.

## 37–44. Validation and audit integrity

The required validation commands are recorded in the sprint response after this report is generated. This audit adds only `docs/GLOBAL_ARCHITECTURE_AUDIT.md`; it performs no source refactor and creates no commit.
