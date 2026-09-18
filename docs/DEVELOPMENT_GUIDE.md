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

`mvn exec:java` starts `com.eyecode.ui.web.LocalWebShellLauncher`, the primary Web composition root. It starts loopback HTTP and WebSocket servers and prints the browser URL; opening the browser is intentionally optional and can be enabled with `-Deyecode.web.openBrowser=true` when `java.awt.Desktop` is supported. JavaFX/CEFFX and Swing/JCEF remain compile-time legacy adapters, not requirements of this runtime.

The Web runtime has no browser-native file or directory picker yet. `workspace/openProject` without a path, `workspace/chooseDirectory`, and Save As return the recoverable `NATIVE_UI_UNAVAILABLE` protocol error. Supplying a path for opening a project remains supported.

Read [Architecture](ARCHITECTURE.md) before choosing a package. Package names communicate ownership; do not place a class in `javafx` or `swing` merely because its first caller is there.

## Onde colocar código novo

| Tipo de feature | Camada/package | Exemplo |
| --- | --- | --- |
| Documento, caret, seleção ou indentação | `editor.intelligence` | `TypingPipeline`, `JavaIndentPolicy` |
| Lexer, parser, AST, semântica | `language` | `JavaLexerService`, `DefinitionAtCaretResolver` |
| Catálogo, sessão, prática ou apresentação de lesson | `lessons` | `LessonContentService`, `PresentationCompiler` |
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
3. Uma nova capacidade de janela entra em `WebShellNativeUi` ou `WebShellDocumentationHost` somente se o boundary for real.
4. Não importe `com.eyecode.swing.*`.

`JavaFxWebDocumentationHost` é o modelo: ele contém layout JavaFX, enquanto `WebShellWorkspaceController` conhece apenas o contrato `WebShellDocumentationHost`.

## Como adicionar uma funcionalidade React

1. Escolha a área em `src/main/web/src` (`workspace`, `document`, `learning`, `lessons` ou `monaco`).
2. Use `bridge` de `bridge/EyeCodeBridge.ts`.
3. Extraia estado/bridge para um serviço ou controller quando mais de um componente precisa da feature.
4. Mantenha edição, decorations e animações em `MonacoWorkspaceService`.
5. Execute `npm run typecheck` e `npm run build`.

Não use `window.cefQuery` em componente React. `EyeCodeBridge` seleciona CEFFX ou WebSocket local e preserva request IDs, timeout e tratamento de erro.

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
| `workspace`, `document`, `run`, `terminal` | `WebShellWorkspaceController` |
| `lessons` | `WebShellLessonsController` |
| `learning` | `WebShellLearningController` |
| `completion` | `WebShellCompletionController` |
| `diagnostics` | `WebShellDiagnosticsController` |
| `shell` | surface/dispatcher bootstrap |

Crie controller novo apenas para um agregado e ciclo de vida novos, não para dividir uma classe arbitrariamente. Receba `WebShellSurface` e contratos de aplicação, nunca `JavaFxWebShellSurface` ou `SwingWebShellSurface`.

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

- `ui.web.monaco`: DTOs e utilitários Java ↔ Monaco.
- `javafx.monaco.JavaFxMonacoEditorSurface`: surface visual CEFFX.
- `MonacoWorkspaceService`: modelos, animações, decorations e recuperação visual React.
- `EyeCodeCompletionService`: adapta o motor de completion aos itens Monaco.

Não crie parser Java paralelo nem replique Presentation Compiler no player Monaco.

## Como adicionar protocolo sem quebrar outras UIs

1. Prefira novo `channel/name` a mudar semântica de mensagem existente.
2. Preserve envelope e campos de correlação.
3. Faça controllers dependerem de `WebShellSurface` e contratos como `WebShellNativeUi`.
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
