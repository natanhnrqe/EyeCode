---
id: java/javafx/stage-scene
title: JavaFX: Stage e Scene
summary: A anatomia de uma janela JavaFX — o palco (Stage), a cena (Scene) e o nó (Node) — e como montar a primeira interface de verdade.
level: beginner
duration: 8
officialDocs:
  label: OpenJFX Documentation
  url: https://openjfx.io/javadoc/
related:
  - java/jdk/fundamentos/classes
  - java/junit/first-test
---

> [!INFO] Toda janela JavaFX é um **Stage** (palco) contendo uma **Scene** (cena), e a cena é uma árvore de **Nodes** (nós — botões, textos, caixas). Metáfora de teatro: palco, cenário, atores.

## Por que existe

Interfaces em Java passaram por muitas gerações (AWT → Swing) e JavaFX (2008) veio trazer o modelo que faltava: **árvore de cenas** (Scene Graph) com CSS, FXML, animações e hardware acelerado por GPU. Cada janela é uma árvore de objetos re-renderizados — você manipula nós como objetos Java, e o framework cuida de desenhar.

## Anatomia da sintaxe

```java
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

public class App extends Application {

    @Override
    public void start(Stage palco) {
        Label rotulo = new Label("Olá, JavaFX!");
        StackPane raiz = new StackPane(rotulo);      // nó container
        Scene cena = new Scene(raiz, 400, 300);      // cena com tamanho

        palco.setTitle("Minha primeira janela");
        palco.setScene(cena);
        palco.show();                                // sobe o palco
    }

    public static void main(String[] args) {
        launch(args);
    }
}
```

Papéis:

| Peça | Papel | Análogo |
|------|-------|---------|
| `Stage` | A janela (título, tamanho, ícone) | O palco |
| `Scene` | O conteúdo raiz + dimensões | A cena |
| `Node` | Cada componente (`Label`, `Button`...) | Atores/cenografia |
| `Application` | Ciclo de vida da UI | A peça em cartaz |

## Como funciona

Ao chamar `launch(args)`, o JavaFX sobe sua **Application Thread** (FX Application Thread) e chama `start(Stage)` com o palco principal. A partir daí:

1. Você monta a árvore de nós — de baixo para cima (cria os filos, adiciona no container);
2. `setScene(cena)` liga a cena ao palco;
3. `show()` exibe a janela e entra no **pulse loop**: o framework redesenha quando o estado muda.

**Regra de ouro:** todo código que toca em nós deve rodar na FX Application Thread. Código de backend que demora deve ser movido para `Task`/`Platform.runLater` — tocar na UI fora dessa thread é a origem clássica de `IllegalStateException` intermitente.

Containers (layouts) definem o posicionamento: `StackPane` (empilha e centraliza), `VBox` (coluna), `HBox` (linha), `BorderPane` (topo/centro/baixo/esquerda/direita), `GridPane` (grade).

## Exemplos

Janela com botão e reação a clique:

```java
@Override
public void start(Stage palco) {
    Label status = new Label("Aguardando...");
    Button botao = new Button("Clique aqui");
    botao.setOnAction(evento -> status.setText("Botão clicado!"));

    VBox raiz = new VBox(12, botao, status);   // espaçamento 12px
    raiz.setStyle("-fx-padding: 24;");

    palco.setScene(new Scene(raiz, 360, 220));
    palco.show();
}
// confira o import: javafx.scene.control.Button
```

Aplicando o visual com CSS — sem sair do Java:

```java
raiz.setStyle("""
        -fx-background-color: #111525;
        -fx-padding: 24;
        """);
rotulo.setStyle("-fx-text-fill: #e3e6ee; -fx-font-size: 18px;");
```

Ou carregue um `.css` externo: `raiz.getStylesheets().add(getClass().getResource("app.css").toExternalForm());`

## Armadilhas comuns

> [!WARNING] Chamar `launch()` mais de uma vez na mesma JVM, ou esquecer que `start` é chamado na FX thread, gera comportamentos estranhos. Aplicação JavaFX tradicional = 1 `launch` por processo (exceto com APIs de janelas secundárias).

**Ciclo de vida errado:** o método correto para sobrescrever é `start(Stage)` — quem tem `main` é só o repassador `launch(args)`. `stop()` existe para limpeza ao fechar.

**Esquecer `show()`:** montar tudo e não chamar `palco.show()` ⇒ nada aparece — o palco existe, só não está visível.

**UI congelada por trabalho pesado:**

```java
// ruim: trava a janela
botao.setOnAction(e -> { List<Dado> dados = carregarDoBancoLento(); });

// certo: trabalho em background, resultado na FX thread
Task<List<Dado>> task = new Task<>() { @Override protected List<Dado> call() {
    return carregarDoBancoLento(); } };
task.setOnSucceeded(ev -> tabela.getItems().setAll(task.getValue()));
new Thread(task).start();
```

**Usar Swing dentro de JavaFX (e vice-versa):** os toolkits não se misturam de forma confiável — escolha um para a UI.

## Profundidade

**Scene Graph:** a árvore de nós é renderizada por primitivas do Prism (GPU) com fallback software. Nós têm propriedades observáveis (`StringProperty`, `DoubleProperty`) — bindings reativos (`label.textProperty().bind(campo.textProperty())`) atualizam a UI automaticamente quando o dado muda, sem ouvintes manuais.

**FXML separa visual de lógica:** `FXMLLoader` carrega um XML declarativo (`BorderPane` com `fx:id` e `onAction`), e o controller Java recebe via `@FXML` — o mesmo padrão MVC de muitas frameworks.

**Threads (JLS não cobre — é regra do toolkit):** FX Application Thread é a única thread "de UI"; `Platform.runLater(runnable)` agenda código nela; `Task`/`Service` + `ExecutorService` cuidam do background com suporte a progresso/cancelamento.

**JDK embutido (JavaFX como módulo):** desde o Java 11 o JavaFX (OpenJFX) é módulo separado — adicione `javafx-controls`, `javafx-graphics` etc. como dependência (Maven: `org.openjfx:javafx-controls:21`) e, se necessário, `--module-path` + `--add-modules`. No JBR (JetBrains Runtime) usado pela EyeCode, o JavaFX já acompanha o runtime.

**Próximos passos:** bindings e properties (dados reativos), CSS dedicado, FXML + controllers, `TableView` com listas observáveis, e service layers com `Task` para I/O.
