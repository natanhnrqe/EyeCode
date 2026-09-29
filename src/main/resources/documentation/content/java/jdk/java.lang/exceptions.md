---
id: java/jdk/java.lang/exceptions
title: Exceções
type: concept
summary: Como o Java sinaliza e trata erros em tempo de execução — hierarquia Throwable, try/catch/finally, exceções verificadas e regras de uso corretas.
level: beginner
duration: 9
officialDocs:
  label: JLS §11 — Exceptions
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-11.html
related:
  - java/jdk/fundamentos/classes
  - java/jdk/java.lang/object
---

> [!INFO] Exceção é um **evento de erro** que interrompe o fluxo normal: quem causa sinaliza com `throw`, quem pode resolver captura com `catch`. Java ainda **obriga** a tratar as *exceções verificadas* — o compilador não deixa o programa compilar sem decidir o que fazer com elas.

## Por que existe

Sem exceções, todo método que pode falhar teria que devolver um "código de erro" e o chamador teria que checar esse código a cada linha — metade do programa viraria tratamento de erro, e esquecer uma checagem passaria despercebido.

Exceções separam os dois caminhos: o código **normal** fica limpo e o **erro** sobe a pilha de chamadas até alguém capturar. Ainda melhor, carrega diagnóstico junto — tipo da falha, mensagem e a pilha de chamadas (*stack trace*) mostrando exatamente onde aconteceu.

```java
try {
    int idade = calcularIdade(dataInvalida);
} catch (DateTimeParseException e) {
    System.out.println("data inválida: " + e.getMessage());
}
```

## Anatomia da sintaxe

```java
try {
    int[] v = new int[2];
    v[5] = 1;                        // lança a exceção
} catch (ArrayIndexOutOfBoundsException e) {
    System.out.println("índice: " + e.getMessage());
} finally {
    System.out.println("sempre roda");   // limpeza garantida
}
```

| Parte | O que é | Exemplo |
|-------|---------|---------|
| `throw` | lança uma exceção **agora** | `throw new IllegalArgumentException("negativo");` |
| `try` | bloco monitorado | `try { ler(); }` |
| `catch` | trata um tipo específico | `catch (IOException e) { ... }` |
| `finally` | limpeza que roda sempre | `finally { arquivo.close(); }` |
| `throws` | declara para quem chama | `void ler() throws IOException` |

A hierarquia nasce em `Throwable` e se divide em três destinos:

| Categoria | Exemplos | O compilador exige tratar? |
|-----------|----------|----------------------------|
| `Error` | `OutOfMemoryError`, `StackOverflowError` | não — problemas de ambiente |
| `RuntimeException` (não verificadas) | `NullPointerException`, `IllegalArgumentException` | não |
| Demais `Exception` (verificadas) | `IOException`, `SQLException`, `InterruptedException` | **sim** — `catch` ou `throws` |

## Como funciona

Quando uma linha lança exceção, o Java executa quatro passos:

1. **Cria** o objeto `Throwable` no ponto do `throw`, capturando ali a pilha de chamadas;
2. **Interrompe** o fluxo normal — as linhas seguintes do bloco não executam;
3. **Procura um `catch` compatível** subindo a pilha (cada método que não tem é "desenrolado");
4. Se **ninguém capturar**, a thread morre e a JVM imprime o stack trace na saída de erro.

Regra das verificadas (JLS §11.2): um método só pode lançar exceção verificada se ela for do tipo declarado em `throws` (ou subtipo) ou se capturar internamente — `RuntimeException` e `Error` ficam de fora da obrigação.

`finally` roda em qualquer saída do `try` — retorno normal, exceção tratada ou não, mesmo com `return` no meio. É o lugar de fechar arquivos e conexões (hoje, o preferido é o try-with-resources).

## Exemplos

Lançando e capturando com mensagem:

```java
static int dividir(int a, int b) {
    if (b == 0) throw new ArithmeticException("divisão por zero");
    return a / b;
}

try {
    System.out.println(dividir(10, 0));
} catch (ArithmeticException e) {
    System.out.println("erro: " + e.getMessage());   // erro: divisão por zero
}
```

Declarando para o chamador decidir e tratando dois tipos de uma vez:

```java
static void carregar(String caminho) throws IOException, SQLException {
    Files.readString(Path.of(caminho));   // pode lançar IOException
    consultaBanco();                      // pode lançar SQLException
}

try {
    carregar("config.txt");
} catch (IOException | SQLException e) {   // multi-catch (Java 7+)
    System.out.println("falha ao carregar: " + e.getMessage());
}
```

Recursos que fecham sozinhos (try-with-resources, Java 7+):

```java
try (BufferedReader in = Files.newBufferedReader(Path.of("config.txt"))) {
    String linha = in.readLine();          // ← se lançar, o arquivo fecha mesmo assim
}                                          // fecha na ordem inversa da criação
```

## Armadilhas comuns

> [!WARNING] `catch (Exception e) { }` vazio engole o erro: o programa segue como se nada tivesse acontecido e o bug aparece bem depois (ou nunca). Capture o tipo mais específico possível e registre o erro num log.

**`finally` sobrescrevendo o retorno:**

```java
static int salvar() {
    try { return 1; }
    finally { return 2; }   // o return do try é DESCARTADO — devolve 2
}
```

Nunca use `return` dentro de `finally` — ele anula a exceção pendente também e some com o valor real.

**Confundir `throw` e `throws`:**

```java
throw new IllegalArgumentException("idade negativa");   // lança AGORA
void ler() throws IOException;                          // só AVISA o chamador
```

Um é comando (executa), o outro é parte da assinatura (promessa de contrato com quem chama).

**Recurso aberto sem try-with-resources:**

```java
BufferedReader in = new BufferedReader(new FileReader(f));
String linha = in.readLine();   // se lançar, o close() abaixo nunca executa
in.close();
```

Quando há mais de um recurso, o correto é aninhar no `try ( ... )` — o fechamento vira garantia da linguagem, não do programador.

## Profundidade

**Verificadas vs não verificadas (JLS §11.1):** a divisão é deliberada. *Não verificadas* (`RuntimeException`) indicam erro de programação — `NullPointerException`, `IndexOutOfBounds`, `ClassCastException` —, que corrigiriam o código, por isso ninguém é obrigado a tratá-las. *Verificadas* indicam problemas externos que o chamador pode plausivelmente recuperar (arquivo ausente, rede fora). A escolha foi controversa desde o dia um: alguns frameworks modernos evitam checadas justamente porque quase sempre acabam sendo repassadas com `throws`.

**Stack trace é diagnóstico, não texto de interface:** `getMessage()` descreve a falha para o desenvolvedor; a mensagem exibida ao usuário deve ser própria (com i18n). `printStackTrace()` vai para `stderr` e serve ao debug — em produção, logue com contexto (nível, timestamp, rastreio).

**Causa raiz (`getCause`):** quando você captura para relatar em outro nível, **preserve a original**: `throw new IOException("falha ao salvar", e)`. Sem isso, o stack trace perde o ponto real da falha e o erro vira "io exception" sem contexto — o pecado mais comum em camadas de serviço. No try-with-resources, a exceção do `close()` não sobrescreve a do corpo: ela vai para `getSuppressed()`.

**`Error` não é para capturar:** `OutOfMemoryError` e `StackOverflowError` dizem que o ambiente não sustenta mais a execução; `catch (Throwable)` esconde isso e deixa o programa num estado imprevisível. Deixe `Error` subir.

**Exceção não é fluxo de controle:** lançar é caro — a JVM captura a pilha inteira no `throw`. Usar exceção para alternar caminhos de negócio (ex.: `try { ... } catch (FimDeLista)`) torna o código lento e ilegível; para controle normal use `if`, `Optional` ou retorno. A regra prática é: exceção para o **inesperado**, retorno para o **esperado**.

**Personalizando:** uma exceção de domínio deve estender `RuntimeException` quando o chamador não tem o que fazer (erro de regra) e uma verificada quando existe tratamento real disponível. Mantenha imutável, exponha dados como campos (`getFaltando()`) e escreva mensagem única — classes de exceção quase sempre são duas linhas e valem a pena.
