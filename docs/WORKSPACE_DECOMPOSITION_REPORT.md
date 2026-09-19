# Workspace controller decomposition / application use cases

## Escopo e resultado

Sprint sobre o working tree recebido, preservando as alterações anteriores. Nenhum commit.
Protocolo `eyecode.web/1`, frontend, resources de lessons, PresentationCompiler,
RunService, TerminalService, JavaFX, Swing, CEFFX, JCEF e arquitetura de linguagem
não foram modificados nesta sprint. O status Git ainda inclui alterações anteriores
nessas áreas; elas não devem ser confundidas com o delta desta sprint.

## 1. Inventário antes/depois

LOC físicos até a última linha de código, incluindo linhas vazias internas.

| Medida | Workspace antes | Workspace depois | Document extraído |
| --- | ---: | ---: | ---: |
| LOC | 891 | 350 | 459 |
| Fields | 21 | 10 | 17 |
| Dependências do construtor completo | 10 | 8 | 6 |
| Handlers | 21 | 14 | 7 |

O construtor anterior também possuía um overload de nove parâmetros, sem documentationHost.
A finalidade não foi minimizar a soma de linhas: foram adicionados modelos neutros,
testes de aplicação e ownership explícito de observações.

Fields anteriores:

- Dependências: surface, nativeUi, manager, diagnosticsController,
  projectLifecycleService, executionController, fileOperations,
  projectCreationService, documentationHost, documentationOpener.
- Helpers: jdkSourceLoader, jdkSourceDeclarationLocator.
- Estado de apresentação: jdkSourceDocuments, documentationDocuments,
  observedDocuments, untitledNames, reidentifyingSessions, nextUntitledNumber.
- Lifecycle: saveListener, externalFileListener, disposed.

Fields finais de Workspace: surface, nativeUi, manager, projects, explorer,
fileOperations, documents, executionController, externalFileListener, disposed.
Os oito primeiros são exatamente os argumentos de construção, sem parameter bag.

Document recebe surface, documentationOpener, documentationHost, nativeUi,
manager e diagnosticsController. Retém os helpers/estado de abas e seus listeners;
não conhece ProjectLifecycleService, criação Maven nem explorer.

### Métodos anteriores agrupados por responsabilidade

| Responsabilidade real | Métodos do inventário anterior |
| --- | --- |
| Lifecycle do adapter | dispose |
| Protocolo de documentos | open, newDocument, activate, change, save, close, documentationLayout |
| Sessões e projeção de documentos | openPath, observe, documentFor, snapshot, sessionFor, sessionForPath, sendActiveChanged(EditorSession) |
| Persistência e UI nativa | saveAs, chooseSaveTarget, onSaved, onExternalChanged |
| Transição/criação de projeto | openProject, openProjectResult, openWorkspace, createProject, requireProject |
| Seleção de diretório | chooseDirectory, chooseDirectoryResult, nativeUiUnavailable |
| Explorer e navegação | workspaceSnapshot, workspaceChildren, refreshWorkspace, openWorkspaceFile, preferredEntryPoint, sourceFor(ProjectModel,String), sourceRoots, isSensibleSource, treeNode, treeChildren, hasVisibleChildren, isVisibleProjectPath |
| Mutação de projeto/sessões | createFile, createDirectory, createJavaClass, createPackage, createInDirectory, duplicatePath, renamePath, deletePath, sessionsUnder |
| Payloads de workspace | workspacePayload, projectPayload, mutationPayload, revealPayload, sendTreeChanged |
| Documentação/JDK | openDocumentationTarget, documentationUri, documentationFor, openJdkSource, sourceFor(Map), sendActiveChanged(WebDocumentationDocument), sendActiveChanged(WebJdkSourceDocument) |
| Coerção/tradução protocolar | text, number, paths, safeMessage |

Tipos internos anteriores: DirectoryMutation, WebDocumentationDocument e
WebJdkSourceDocument. Os dois records de abas ficaram no adapter de documentos;
DirectoryMutation continua privado ao adapter de workspace. DocumentObservation
foi acrescentado para guardar e remover os listeners exatos registrados.

### Dependências e chamadas anteriores

- Filesystem: Files.isRegularFile, Files.isDirectory, Files.walk (limite 12),
  Files.list; normalização/ancestrais por Path. Traversal e regras de visibilidade
  migraram para ProjectExplorerQuery. Verificação de arquivo e destino já aberto
  ficam no EditorManager; controllers não referenciam Files.
- EditorManager: openDocument, activateSession, getSessions, getCurrentSession,
  getBuffer, flushSession, saveAs, renamePath, deletePath, closeSession,
  closeAllSessions, watchProject, add/removeSaveListener,
  add/removeExternalFileListener. A sequência closeAllSessions/watchProject saiu
  para WorkspaceProjects. Proteções de rename/delete foram movidas ao manager.
- ProjectLifecycleService: currentProject, open, recordRecent, recentProjects.
  O adapter de workspace agora acessa a capability WorkspaceProjects.
- Protocolo: Map/LinkedHashMap, WebDocumentSnapshot.payload, MonacoModelId,
  WebShellEnvelope.response/event/error, WebShellError, coerção de strings,
  números e coleções. Permanecem exclusivamente Web.
- Surface: 21 registros de handlers e envio de respostas assíncronas, eventos
  document/opened/activeChanged/changed/reidentified/closed/saved/saveFailed/
  externalChanged e workspace/reset/changed/treeChanged. Os canais foram preservados.

## 2. Classificação dos 21 handlers

Cada linha separa transporte, comportamento de aplicação, estado/regra e infraestrutura.
Não há um UseCase.java por request.

| Handler | Transporte/adapter | Aplicação | Estado/regra | Infraestrutura |
| --- | --- | --- | --- | --- |
| document/open | path/URI, resposta/snapshot | EditorManager | identidade/sessão | FileSystemService |
| document/new | conteúdo/nome exibido | EditorManager | nova sessão sem arquivo | buffer/autosave |
| document/activate | URI/documentId, evento | EditorManager | sessão ativa; abas não-file no adapter | host de documentação opcional |
| document/change | conteúdo/versão otimista, conflito | EditorDocument via manager | texto/versão/dirty | pipeline de eventos existente |
| document/save | URI, picker/erros, reidentified | EditorManager | flush/saveAs e colisão de sessão | autosave/FileSystemService/native picker |
| document/close | URI, evento closed | EditorManager | encerramento da sessão; remoção de observação | autosave/dispose |
| document/layout | geometria Web | não há use case de negócio | layout da aba de documentação | WebShellDocumentationHost |
| workspace/snapshot | payload project/recentProjects | WorkspaceProjects + ProjectExplorerQuery | projeto atual/recente e nó raiz | ProjectService/NIO |
| workspace/openProject | path/picker, reset/changed | WorkspaceProjects.open | transição de workspace | lifecycle/editor/run/watch |
| workspace/createProject | name/location/groupId | WorkspaceProjects.create | criar e abrir projeto Maven | MavenProjectCreationService |
| workspace/chooseDirectory | resposta/cancelamento/indisponível | não há use case de negócio | escolha do usuário | native picker |
| workspace/refresh | paths/validPaths | ProjectExplorerQuery | expansão válida e fallback raiz | NIO |
| workspace/children | parent/children/erro | ProjectExplorerQuery | visibilidade, ordenação, hasChildren | NIO lazy traversal |
| workspace/openFile | path/snapshot | ProjectExplorerQuery + EditorManager | arquivo dentro do projeto/sessão | NIO/FileSystemService |
| workspace/createFile | target/name e mutation payload | ProjectFileOperationService | nome/contenção/colisão | NIO |
| workspace/createDirectory | target/name e mutation payload | ProjectFileOperationService | nome/contenção/colisão | NIO |
| workspace/createJavaClass | target/name e mutation payload | ProjectFileOperationService | identificador Java/template/package | NIO |
| workspace/createPackage | target/name e mutation payload | ProjectFileOperationService | segmentos Java/source root | NIO |
| workspace/rename | target/name, URIs anteriores/reidentified | EditorManager.renamePathSafely | salvar dirty antes de rename/rebind | serviço compartilhado/autosave/NIO |
| workspace/delete | target, closed/treeChanged | EditorManager.deletePathSafely | rejeitar dirty antes de excluir/fechar | serviço compartilhado/autosave/NIO |
| workspace/duplicate | target e mutation payload | ProjectFileOperationService | nome livre/cópia de arquivo | NIO |

## 3. Decisões e justificativas

### Documentos

EditorManager continua sendo a boundary de aplicação. Não foi criado DocumentService.
Ativação, dirty, salvamento e file backing não receberam estado concorrente.
As novas operações seguras de rename/delete retornam PathMutationResult
(SUCCESS, SAVE_FAILED, DIRTY_DOCUMENTS, FAILED); regras não dependem de códigos Web.
As APIs antigas foram preservadas para compatibilidade dos consumidores existentes.
As guardas seguras são o caminho usado pelo Web, não uma mudança silenciosa dos
contratos legados de delete/rename.

WebShellDocumentController é uma extração de adapter, não uma nova camada de domínio.
Seu cluster tem sete handlers, estado de abas e observações próprias. Workspace
usa explicitamente sua projeção ao abrir arquivos, reidentificar e resetar abas.
Documentação e JDK source continuam nesse cluster: são non-file tabs, read-only,
com ativação/fechamento compartilhados. Não justificam serviço paralelo nesta sprint.

### Explorer

Era mistura de query de aplicação, traversal e projeção Web. ProjectExplorerQuery
retém as regras reutilizáveis e NIO concreto; Entry(Path,name,directory,hasChildren)
é um resultado neutro. Workspace preserva a codificação JSON, incluindo kind=project
somente para a raiz. Filtros/ordem/fallbacks existentes foram mantidos. A contenção
da query continua lexical por Path normalizado; não foi apresentada como sandbox
de symlinks. A mutação mantém suas próprias guardas existentes de filesystem.

### Projeto e operações de arquivo

WorkspaceProjects extrai a sequência stop → lifecycle.open/recordRecent →
editor.closeAllSessions/watchProject → refreshConfigurations. A criação Maven
entra na mesma sequência. ProjectLifecycleService mantém ownership do modelo atual
e recents. MavenProjectCreationService mantém template, validação e escrita.
Não foram alterados contratos internos desses serviços, nem criada abstração Gradle.

ProjectFileOperationService já era coeso para create/duplicate/rename/delete de
filesystem. Continua sendo uma única instância compartilhada com EditorManager.
O gap não era mais um wrapper de arquivos, mas a proteção de sessões dirty:
ela foi transferida ao dono dessas sessões.

### Protocolo e erros

WebShellPayload centraliza text/number/paths sem alterar coerção. Não surgiram DTOs
Web na aplicação; schemas de requests não justificaram records adicionais.
Mapas de envelope e snapshots continuam nos adapters. As traduções preservam:

- INVALID_DOCUMENT, DOCUMENT_NOT_FOUND, DOCUMENT_NOT_OPEN, DOCUMENT_UNAVAILABLE;
- DOCUMENT_VERSION_CONFLICT, DOCUMENT_READ_ONLY, DOCUMENT_ALREADY_OPEN;
- NEW_DOCUMENT_FAILED, SAVE_FAILED, SAVE_AS_FAILED, CLOSE_FAILED;
- INVALID_PROJECT, PROJECT_CREATION_FAILED, INVALID_TREE_PATH;
- NATIVE_UI_UNAVAILABLE, NATIVE_UI_FAILED e respostas cancelled;
- CREATE_FILE, CREATE_DIRECTORY, CREATE_JAVA_CLASS, CREATE_PACKAGE;
- DUPLICATE_FAILED, RENAME_SAVE_FAILED, RENAME_FAILED, DIRTY_DOCUMENTS, DELETE_FAILED.

IOException/IllegalArgumentException dos serviços concretos são traduzidas na
boundary. Não foi criada hierarquia gigante de exceptions nem alterado o envelope.

### Justificativa por classe extraída

| Classe | Responsabilidade | Reutilizador | Dependência removida | Teste |
| --- | --- | --- | --- | --- |
| WorkspaceProjects | transição/criação de workspace | outro frontend/automação/headless | workflow deixa de depender de Web controllers | WorkspaceProjectsTest sem Surface |
| ProjectExplorerQuery + Entry | semântica de navegação | outro explorer/frontend | traversal e filtros deixam o Web | ProjectExplorerQueryTest com TempDir |
| WebShellDocumentController | adapter/observações de abas | todas as surfaces eyecode.web/1 | workspace deixa de conhecer hosts/JDK loaders/dirty listeners | WebShellWorkspaceDocumentsTest |
| WebShellPayload | coerção protocolar comum | adapters de documento/workspace | duplicação local, sem vazar DTO | contratos exercitados pelos testes Web |

Rejeitados: DocumentService duplicando EditorManager; wrapper genérico de file
operations; um use case por handler; context/registry/service bag; frontend-specific
application DTOs; abstração Maven/Gradle; controller separado apenas para JDK tabs.

## 4. Composição, ownership e renovabilidade

```text
WebShellWorkspaceComposition
  ├─ WorkspaceProjects → ProjectLifecycleService, EditorManager, RunService, MavenProjectCreationService
  ├─ ProjectExplorerQuery → ProjectModel, NIO
  ├─ WebShellWorkspaceController → projects, explorer, fileOperations, manager, documents, execution, surface, picker
  ├─ WebShellDocumentController → manager, diagnostics, surface, picker, documentation host/opener
  └─ WebShellWorkspaceRuntime
       ├─ workspace, document, completion, learning, lessons, diagnostics, execution controllers
       └─ WorkspaceApplication → lifecycle dos serviços compartilhados
```

Controllers finais: Workspace, Document, Execution, Completion, Learning, Lessons,
Diagnostics; Native continua sendo adapter de controles de janela das surfaces.
Runtime fecha controllers antes dos serviços. As duas capabilities novas não possuem
threads/listeners/recursos próprios e não recebem close artificial. Instâncias são
construídas na composition e retidas por seus consumidores.

Um frontend novo usando eyecode.web/1 reutiliza os mesmos adapters. Um frontend com
outro transporte reutiliza WorkspaceProjects, ProjectExplorerQuery, EditorManager
e ProjectFileOperationService; reimplementa somente apresentação/adaptação.

## 5. Correção adicional evidenciada durante validação

A primeira execução focada bloqueou em LocalWebShellRuntimeTest. Thread dump
confirmou inversão de locks: main segurava connectionLock e aguardava WebSocketImpl;
WebSocketSelector segurava WebSocketImpl e aguardava connectionLock em onClose.
LocalWebShellSurface.close agora destaca a conexão sob lock e chama socket.close
fora dele. Correção pontual de lifecycle, sem mudar transporte/protocolo.
O teste surfaceClosesSocketOutsideConnectionLock protege precisamente esse contrato.
O fork Surefire bloqueado foi encerrado; nenhum processo preexistente do usuário
foi encerrado por essa correção.

## 6. Testes e validações

- 19 testes acrescentados: 7 ExplorerQuery, 5 WorkspaceProjects/EditorManager,
  4 contratos/observações Web, 2 invariantes arquiteturais e 1 regressão de lock.
- Testes de aplicação usam serviços reais, fixture TempDir e editor headless,
  sem WebShellSurface. Testes Web usam composição real ou surface capturadora.
- ArchitectureBoundaryTest: 9 testes; application/Entry sem UI/protocolo, controllers
  sem traversal Files, sem construir serviços/siblings, dependências explícitas.
- Focados antes do último teste de lock: 31 testes, 0 failures, 0 errors, 0 skipped.
- mvn test completo: **2160 tests, 0 failures, 0 errors, 7 skipped; BUILD SUCCESS**,
  exit code 0, processo terminou normalmente em 2m52s. Log: target/workspace-full.log.
- A contagem acima é a do Maven desta execução, não os 2172 informados como baseline.
  Existem 14 XMLs antigos de packages JavaFX migrados (80 testes históricos) em
  surefire-reports, não executados neste log. Não foi usada soma indiscriminada dos
  XMLs. Nenhum teste foi removido nesta sprint; não se afirma reconciliação exata
  do número histórico sem o log correspondente.
- npm run typecheck: PASS, exit 0.
- npm run build: PASS, exit 0; nenhum source React/resource de lesson alterado.
- git diff --check: PASS.
- mvn exec:java: bootstrap Web normal; HTTP 200 para HTML/JS/CSS; ping/pong real.
- React: criação e abertura do projeto temporário WorkspaceValidation.
- Monaco: Main.java editado/salvo pela UI, conteúdo confirmado no disco e reaberto
  pelo protocolo em uma segunda execução do runtime.
- Project Run: stdout `Workspace decomposition OK`, exit code 0, observado na UI
  e novamente via WebSocket.
- Lessons: catálogo Java/Fundamentos, abertura da lesson Seu primeiro programa Java
  e avanço até Mostrar uma mensagem; Monaco confirmou o println esperado. O valor
  acessível do editor só foi atualizado ao receber foco; não houve mudança em Lessons.
- Projeto manual de validação preservado em diretório temporário dedicado, fora
  do repositório; script de smoke em target/validate-workspace-runtime.ps1.

## 7. Documentação e Git

ARCHITECTURE.md e DEVELOPMENT_GUIDE.md descrevem as novas boundaries, o fluxo de
projeto, tabela de canais, testes, composição e lifecycle. Este documento guarda
o inventário e as decisões da sprint.

Delta desta sprint: WorkspaceController, novo DocumentController/WorkspaceProjects/
ProjectExplorerQuery/WebShellPayload, composição/runtime, guardas do EditorManager,
publicação de estado do ExecutionController, fechamento da LocalWebShellSurface,
testes correspondentes e documentação. Os demais arquivos sujos recebidos foram
preservados. Nenhum commit, reset, stash ou checkout destrutivo.

Status final: 19 arquivos tracked modificados, 17 untracked e nenhum staged.
A lista inclui o trabalho recebido; não representa 36 arquivos alterados nesta sprint.
