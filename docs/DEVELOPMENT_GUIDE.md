# EyeCode Development Guide

## Getting started

Use Java 21-compatible Maven compilation from the repository root:

~~~powershell
mvn exec:java
mvn test
Push-Location src/main/web
npm run typecheck
npm run build
Pop-Location
~~~

`mvn exec:java` starts `com.eyecode.ui.web.LocalWebShellLauncher`, the primary Web entrypoint. `LocalWebShellRuntime` creates the HTTP/WebSocket surface and `WebShellWorkspaceComposition` visibly assembles services, the lifecycle owner and Web controllers. Opening the browser is intentionally optional and can be enabled with `-Deyecode.web.openBrowser=true` when `java.awt.Desktop` is supported. JavaFX/CEFFX and Swing/JCEF remain compile-time legacy adapters, not requirements of this runtime.

The Web runtime has no browser-native file or directory picker yet. `workspace/openProject` without a path, `workspace/chooseDirectory`, and Save As return the recoverable `NATIVE_UI_UNAVAILABLE` protocol error. Supplying a path for opening a project remains supported.

Read [Architecture](ARCHITECTURE.md) before choosing a package. Package names communicate ownership; do not place a class in `javafx` or `swing` merely because its first caller is there.

Read the [architecture decisions](adr/) when a change affects runtime ownership or dependency direction.

## Onde colocar código novo

| Tipo de feature | Camada/package | Exemplo |
| --- | --- | --- |
| Documento, caret, seleção ou indentação | `editor.intelligence` | `TypingPipeline`, `JavaIndentPolicy` |
| Lexer, parser, AST, semântica | `language` | `JavaLexerService`, `DefinitionAtCaretResolver` |
| Catálogo, sessão, prática ou apresentação de lesson | `lessons` | `LessonContentService`, `PresentationCompiler` |
| Serviço que compartilha lifecycle de uma workspace | `application` + serviço dono | `WorkspaceApplication` |
| Workspace, projeto, execução ou arquivo | `workbench`, `project`, `runtime`, `filesystem` | `EditorManager`, `RunService` |
| Evento interno em processo | `eventbus` e domínio dono | `DocumentTextChangeEvent` |
| Contrato Java ↔ React/Monaco | `ui.web`, `ui.web.monaco`, `ui.web.learning` | `WebShellEnvelope`, `MonacoCommand` |
| Surface JavaFX/CEFFX | `javafx` | `JavaFxWebShellSurface`, `JavaFxMonacoEditorSurface` |
| Compatibilidade Swing | `swing` ou legado `ui` | `SwingWebShellSurface` |
| React, Monaco e apresentação visual | `src/main/web/src` | `EyeCodeBridge`, `MonacoWorkspaceService` |

## Como adicionar uma funcionalidade ao Core

1. Coloque a regra em `editor.intelligence`, `language` ou no modelo de domínio correspondente.
2. Faça a API aceitar contratos/modelos Core, não `Node`, `JPanel`, `WebShellEnvelope` nem DTO Monaco.
3. Cubra a regra com teste unitário antes de adaptá-la para UI.
4. Deixe a aplicação/UI traduzir entradas e saídas.

Por exemplo, Smart Editing lê `DocumentSnapshot` e grava por `EditorCommandContext`; ele nunca acessa um componente Swing ou JavaFX.

## Como compor uma feature de workspace

`WorkspaceApplication` é owner de lifecycle compartilhado, não factory nem service locator. A composição escolhe implementações concretas; controllers recebem serviços e capabilities que realmente usam.

1. Coloque a regra no serviço dono (`project`, `runtime`, `workbench`, `filesystem` ou domínio), com teste focado.
2. Se a regra precisa encerrar junto da workspace, passe seu serviço ao construtor de `WorkspaceApplication`; não acrescente getter para pescá-lo depois.
3. Faça `WebShellWorkspaceComposition` escolher o filesystem, `EventBus`, serviços concretos e adapters, como `WebShellEditorViewFactory`.
4. Faça o controller Web adaptar envelope → serviço; não construa `EditorManager`, `RunService`, `TerminalService` ou filesystem no controller, nem receba `WorkspaceApplication`.
5. `WebShellWorkspaceRuntime` possui controllers e os encerra antes de `WorkspaceApplication.close()`. Todo listener deve ser removido pelo controller que o registrou.

Não use container DI, singleton global ou uma interface nova caso não exista uma implementação substituível e um consumidor que precise deixar de conhecê-la.

## Como adicionar uma funcionalidade Swing

Swing é compatibilidade. Primeiro confirme que a regra não pertence ao Core ou ao Web Shell compartilhado.

1. Coloque a implementação visual em `com.eyecode.swing` ou no componente legado responsável.
2. Para uma capacidade Web Shell, implemente/adapte `WebShellNativeUi` ou `WebShellSurface`; não bifurque o controller de workspace.
3. Não importe `com.eyecode.javafx.*`.
4. Teste a regra fora do toolkit quando possível.

`SwingWebShellSurface` é o padrão: possui JCEF/AWT, mas usa envelope e dispatcher compartilhados.

## Como adicionar uma funcionalidade JavaFX

1. Deixe layout, `Node`, `Region`, `Platform.runLater` e CEFFX em `com.eyecode.javafx`.
2. Para falar com React, use `WebShellSurface` e DTOs de `ui.web`; não crie uma cópia JavaFX do protocolo.
3. Uma nova capacidade de seleção entra em `WebShellNativeFileSelection`; uma capacidade de janela entra em `WebShellWindowControls`; documentação usa `WebShellDocumentationHost`. Só crie uma capability quando houver consumidor e adapter reais.
4. Não importe `com.eyecode.swing.*`.

`JavaFxWebDocumentationHost` é o modelo: ele contém layout JavaFX, enquanto `WebShellDocumentController` conhece apenas o contrato `WebShellDocumentationHost`.

## Como adicionar uma funcionalidade React

1. Escolha a área em `src/main/web/src` (`workspace`, `document`, `learning`, `lessons` ou `monaco`).
2. Use `bridge` de `bridge/EyeCodeBridge.ts`.
3. Extraia estado/bridge para um serviço ou controller quando mais de um componente precisa da feature.
4. Mantenha edição, decorations e animações em `MonacoWorkspaceService`.
5. Execute `npm run typecheck` e `npm run build`.

Não use `window.cefQuery` em componente React. `EyeCodeBridge` seleciona CEFFX ou WebSocket local e preserva request IDs, timeout e tratamento de erro.

React é organizado por responsabilidade natural, não como um espelho dos packages Java:

- `bridge/`: protocolo e transporte;
- `workspace/`, `document/`, `diagnostics/`, `learning/`, `lessons/`, `completion/`: features e componentes;
- `monaco/MonacoWorkspaceService.ts`: boundary de editor, modelos, decorations e presentation.

Trocar React afetaria esses adapters/frontend e o contrato `eyecode.web/1`, não os motores Java de linguagem, editor ou lessons. Trocar Monaco afeta principalmente `MonacoWorkspaceService` e seus componentes consumidores; não mova parsing ou `PresentationCompiler` para TypeScript.

## Como adicionar um evento

EventBus serve fatos internos em processo, não RPC Web.

1. Crie um tipo imutável que implemente `com.eyecode.eventbus.Event`.
2. Publique no serviço que produz o fato:

~~~java
eventBus.publish(new TokensUpdatedEvent(snapshot));
~~~

3. Assine no dono da reação e guarde o token:

~~~java
subscription = eventBus.subscribe(TokensUpdatedEvent.class, this::refresh);
~~~

4. No ciclo de vida de encerramento, chame `eventBus.unsubscribe(subscription)`.

O EventBus entrega no mesmo thread, por tipo exato e na ordem de assinatura. Uma exceção de handler volta ao publisher e impede handlers posteriores. `LexerEventBridge` e `ParserEventBridge` são exemplos: recebem `DocumentTextChangeEvent` e publicam snapshots derivados. Não crie EventBus global ou paralelo.

## Como adicionar ou modificar protocolo Web

O envelope é `eyecode.web/1`: Java usa `WebShellEnvelope` e TypeScript usa `bridge/protocol.ts`. Não o duplique em componentes.

### Ação React → Java

1. Defina o payload TypeScript perto do domínio, por exemplo em `lessons/protocol.ts`.
2. Envie pelo bridge:

~~~ts
const result = await bridge.request<Result>('workspace', 'myAction', { id });
~~~

3. Registre no controller que possui o agregado:

~~~java
surface.registerHandler("workspace", "myAction", this::myAction);
~~~

4. Valide payload e responda a partir da request:

~~~java
private WebShellEnvelope myAction(WebShellEnvelope request) {
    return request.response(Map.of("accepted", true));
}
~~~

5. Adicione teste de controller/codec e mantenha `channel/name` estáveis.

### Informação Java → React

1. Escolha o controller que produz o fato.
2. Envie um evento estruturado:

~~~java
surface.send(WebShellEnvelope.event("workspace", "changed", payload));
~~~

3. Faça o serviço React assinar `bridge.subscribe(...)` e filtrar `channel/name`.
4. Atualize os tipos TypeScript e cubra o controller Java.

Responses usam `requestId`; events não exigem resposta. Use `WebShellError` para erros recuperáveis. Não altere a versão `eyecode.web/1` sem compatibilidade explícita entre Java e frontend.

## Como adicionar controller ou handler

Prefira o controller atual do agregado:

| Canal | Controller atual |
| --- | --- |
| `workspace` | `WebShellWorkspaceController` |
| `document` | `WebShellDocumentController` |
| `run`, `terminal` | `WebShellExecutionController` |
| `lessons` | `WebShellLessonsController` |
| `learning` | `WebShellLearningController` |
| `completion` | `WebShellCompletionController` |
| `diagnostics` | `WebShellDiagnosticsController` |
| `shell` | surface/dispatcher bootstrap |

Crie controller novo apenas para um agregado e ciclo de vida novos, não para dividir uma classe arbitrariamente. Receba `WebShellSurface` e contratos de aplicação, nunca `JavaFxWebShellSurface` ou `SwingWebShellSurface`. `WebShellExecutionController` é o exemplo: possui o cluster coeso de listeners e projeções dos canais `run`/`terminal`.

## Boundaries de workspace, documentos e explorer

Antes de acrescentar uma regra de workspace ao Web adapter, identifique o dono:

- Estado/salvamento/sessões: `EditorManager`. Para rename/delete com proteção de documentos abertos, use `renamePathSafely`/`deletePathSafely`; o resultado neutro é traduzido para os códigos Web existentes.
- Abrir/criar workspace: `WorkspaceProjects`, que coordena lifecycle, editor, execução e a criação Maven concreta.
- Consulta do explorer: `ProjectExplorerQuery`, que retorna `Entry`/`Path`, nunca `Map` Web. Filtros, ordenação, ancestrais e escolha do arquivo inicial são reutilizáveis.
- Criar arquivo/diretório/classe/package e duplicar: use diretamente o `ProjectFileOperationService` compartilhado.
- Parsing, URIs Monaco, envelopes, erros de protocolo e payloads: controllers Web e `WebShellPayload`.
- Abas de documentação/JDK: `WebShellDocumentController`, compartilhando o lifecycle de abas sem transformá-las em arquivos editáveis.

Monte capabilities/controllers em `WebShellWorkspaceComposition`. Controllers com listeners são possuídos e encerrados por `WebShellWorkspaceRuntime`; capabilities sem recursos próprios não precisam de `close` artificial. Não crie context/service bags nem um use case por handler.

Teste regras de aplicação sem `WebShellSurface`, usando `@TempDir` e serviços reais. Teste adaptação com uma surface capturadora, preservando nomes/códigos de `eyecode.web/1`. Exemplos: `ProjectExplorerQueryTest`, `WorkspaceProjectsTest` e `WebShellWorkspaceDocumentsTest`. A guarda arquitetural cobre dependência application → Web, criação indevida de serviços/siblings nos adapters e ausência de traversal NIO nos controllers.

## Como adicionar uma lesson

1. Crie/edite o resource no catálogo de lessons.
2. Declare título, passos, prática e `canonicalCode` conforme `lessons.content`.
3. Uma lesson comum declara apenas estado canônico e transition.
4. Não declare comandos Monaco, ranges, linhas, colunas ou cadence no resource.
5. Execute testes de conteúdo/session/presentation e o teste Web Shell relevante.

O autor descreve intenção e estado final. O compilador decide as operações; o player as apresenta.

## Como adicionar uma presentation

`LessonContentService` compila estados canônicos consecutivos com `PresentationCompiler` e fornece `PresentationProgram` ao frontend.

1. Defina o próximo `canonicalCode`.
2. Use `INSTANT` apenas quando a troca deve materializar sem animação.
3. Atualize `PresentationCompilerTest`.
4. Confirme que o programa reproduz exatamente `targetCode`.

`JavaPresentationChangeAnalyzer`, `PresentationTransitionPlanner` e `PresentationProgramExecutor` compõem o pipeline. Uma nova strategy só se justifica quando há uma transição cuja qualidade não pode ser expressa pela regra geral; strategy não é pré-requisito para animação.

## Como adicionar practice, highlight ou reveal

Practice pertence a `LessonSession` e `WebShellLessonsController`; React exibe estado e envia verificação. Highlights e reveal pertencem a `PresentationProgram` e `MonacoWorkspaceService`, nunca ao resource por coordenadas manuais.

Quando uma operação precisa de informação estrutural, melhore analisador/planner e teste estado intermediário e estado canônico final.

## Como trabalhar com Monaco

Monaco é renderização/frontend. O backend decide modelos, conteúdo, transitions, completion e semântica; o frontend aplica essas decisões.

- `ui.web.monaco`: DTOs e utilitários Web ↔ Monaco.
- `javafx.monaco.JavaFxMonacoEditorSurface`: surface visual CEFFX.
- `MonacoWorkspaceService`: modelos, animações, decorations e recuperação visual React.
- `EyeCodeCompletionService`: adapter legado de completion para itens Monaco; o fluxo Web ativo usa `CompletionService` e resultados neutros.

Não crie parser Java paralelo nem replique Presentation Compiler no player Monaco.

## Como evoluir build, execução e linguagens

`RunService` é o lifecycle de execução consumido pela UI. A resolução atual em `ProjectExecutionResolver` e `MavenClasspathResolver` é Maven-concreta; não introduza uma “BuildSystem” genérica enquanto não houver um segundo executor real (por exemplo, Gradle) e um consumidor que possa depender do contrato comum. Ao adicionar Gradle de verdade, extraia a capability de descoberta/resolução a partir das operações que os dois executores compartilham e mantenha Maven como adapter.

`FileSystemService` já é o port de persistência do editor. `Path` continua sendo um valor JDK válido; não o esconda. `ProjectFileOperationService` ainda é uma implementação NIO coesa e não deve receber um wrapper apenas por consistência visual.

### Adding a language

Não existe um “language service” universal: cada capacidade é registrada e usada separadamente. A implementação atual é Java; `JavaLexerService`, parser, AST, semântica, indentação e seleção continuam contratos Java porque não há um segundo consumidor real que justifique generalizá-los.

Para adicionar uma linguagem que precise de completion ou diagnósticos:

1. Declare o `LanguageId` e registre suas extensões em `WebShellWorkspaceComposition` através de `ExtensionDocumentLanguageResolver`.
2. Para documentos sem path, como `lesson://`, envie o `language` declarado ao criar o modelo Monaco. O frontend envia o ID atual do modelo em cada request; a identidade declarada tem precedência sobre extensão.
3. Implemente tokenização, parser ou semântica somente para as capacidades que a linguagem realmente oferecer. Não force essas APIs Java existentes a se tornarem universais antes de haver outra implementação e consumidor.
4. Para diagnósticos, implemente `DiagnosticsProvider` e registre-o no `DiagnosticsService` da composição.
5. Para completion, implemente `CompletionProvider` e registre-o no `CompletionService` da composição.
6. Para navegação, edição inteligente ou outra capacidade, crie um contrato pequeno quando houver consumidor real; `EditorManager` recebe `EditorIntelligence` para lifecycle/definição, e a composição escolhe o adapter.
7. Cubra resolução de identidade, roteamento da capability, documento sem extensão e resultado desconhecido. Cubra também stale-result se a nova capability for assíncrona no Web Shell.

Não altere `WebShellCompletionController`, `WebShellDiagnosticsController`, envelopes, DTOs Monaco ou `MonacoWorkspaceService` para introduzir decisões específicas de uma linguagem. Eles transportam `LanguageDocument` e resultados neutros. Não crie ServiceLoader, plugin framework ou um parser paralelo em TypeScript.

## Como adicionar protocolo sem quebrar outras UIs

1. Prefira novo `channel/name` a mudar semântica de mensagem existente.
2. Preserve envelope e campos de correlação.
3. Faça controllers dependerem de `WebShellSurface` e da menor capability necessária (`WebShellNativeFileSelection`, `WebShellWindowControls` ou `WebShellDocumentationHost`), não do adapter desktop completo.
4. Atualize React e testes do transporte relevante.
5. Execute `ArchitectureBoundaryTest` para garantir que contratos compartilhados não importam toolkit.

## Como testar uma mudança

Rode primeiro o menor grupo que prova a alteração e depois a validação completa:

~~~powershell
mvn test "-Dtest=ArchitectureBoundaryTest,SeuTeste"
mvn test
Push-Location src/main/web
npm run typecheck
npm run build
Pop-Location
git diff --check
~~~

Inclua testes de estado intermediário quando uma apresentação/typing depende de sequência e teste recuperação ao estado canônico final.

## Checklist antes de PR

- [ ] O package expressa ownership correto.
- [ ] Core/Lessons não importam Swing, JavaFX, CEFFX, JCEF ou Web Shell.
- [ ] Alteração Web usa `WebShellEnvelope`/ `EyeCodeBridge`, não transporte paralelo.
- [ ] Subscrições EventBus guardam e liberam `SubscriptionToken`.
- [ ] Lesson declara estado canônico, não comandos Monaco.
- [ ] Testes focados e `mvn test` passaram.
- [ ] `npm run typecheck`, `npm run build` e `git diff --check` passaram.
- [ ] Bundle gerado não foi editado manualmente.

## APIs deprecated

| API | Use no lugar |
| --- | --- |
| `MarkdownToHtmlConverter` | `FlexmarkConverter`/pipeline HTML atual |
| `BrowserManager#getClient()` | `BrowserManager#createClient()` |
| `LearningDocumentView` e `LearningChromiumView` | composição Web Shell/JavaFX ativa, não uma tela Swing nova de Learn |

`LearningWebView` é legado mas não tem substituto suficientemente comprovado para uma nova anotação. Preserve-o até uma migração de compatibilidade planejada.

## Anti-patterns do EyeCode

- Core importando UI, Monaco, CEFFX, JCEF, Swing ou JavaFX.
- Resource de lesson contendo command/range/linha/coluna Monaco.
- Switch por ID de lesson em vez de estado/transição.
- Componente React acessando `window.cefQuery` quando `EyeCodeBridge` existe.
- Criar EventBus global, bridge paralelo ou parser Java paralelo.
- Duplicar `WebShellEnvelope` ou tipos de protocolo.
- Colocar regra de projeto, parsing ou LessonSession no player Monaco.
- Criar interface/factory apenas para “cumprir SOLID”.
