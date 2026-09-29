---
id: java/jdk/java.lang/enums
title: Enums
type: concept
summary: Tipos com conjunto fixo de constantes — anatomia da declaração, constantes com estado e comportamento, EnumSet e as armadilhas do ordinal.
level: beginner
duration: 8
officialDocs:
  label: JLS §8.9 — Enum Types
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.9
related:
  - java/jdk/fundamentos/classes
  - java/jdk/java.util/set
---

> [!INFO] `enum` declara um **tipo com conjunto fixo de constantes** (`Status`, `Dia`, `Cor`). É uma classe especial: pode ter campos, métodos e construtor — mas **não pode ser estendida** e cada constante é um objeto **único**.

## Por que existe

A alternativa antiga era número solto: `static final int PENDENTE = 1;`. O compilador aceitava qualquer `int`, `switch (status)` com valor 99 compilava, e ninguém entendia o que `if (s == 2)` queria dizer seis meses depois.

Enum transforma essas "categorias" em **tipos de verdade**: só as constantes declaradas existem, comparar é seguro, `switch` pode cobrir todos os casos — e o conjunto de valores fica visível no próprio código, sem precisar de comentário explicando.

## Anatomia da sintaxe

Declaração simples:

```java
enum Prioridade {
    BAIXA, NORMAL, ALTA          // constantes separadas por vírgula
}
```

Com estado e comportamento — a forma completa:

```java
enum Cor {
    VERMELHO("FF0000"),
    VERDE("00FF00");             // ";" encerra as constantes quando há corpo

    private final String hex;               // estado por constante
    Cor(String hex) { this.hex = hex; }     // construtor é sempre privado
    public String hex() { return hex; }     // comportamento comum
}
```

| Parte | O que é | Exemplo |
|-------|---------|---------|
| tipo | a classe enum em si | `enum Status` |
| constantes | os valores possíveis | `PENDENTE, APROVADO, REJEITADO` |
| `;` final | separa constantes do corpo | `A, B;` |
| construtor | declarado sem visibilidade (privado) | `Status(String rotulo)` |
| membros | campos e métodos comuns | `private final String rotulo;` |

O compilador gera automaticamente alguns membros:

| Membro gerado | Retorna | O que faz |
|---------------|---------|-----------|
| `values()` | `Status[]` | array com as constantes (**cópia**) |
| `valueOf(String)` | `Status` | constante pelo nome exato |
| `name()` | `String` | nome da constante |
| `ordinal()` | `int` | posição (0, 1, 2...) |
| `compareTo(E)` | `int` | ordem pela ordem de declaração |

## Como funciona

O compilador reescreve `enum Status { ... }` como uma classe **final** que estende `java.lang.Enum<Status>` — por isso enum não herda de outra classe nem aceita `extends`. Cada constante vira um objeto **estático único**, criado na carga da classe, e é justamente isso que permite comparar com `==` sem medo.

Como as constantes são objetos distintos e imutáveis, `equals`, `hashCode` e `compareTo` são **finais** (herdados de `Enum`) e não podem ser sobrescritos: igualdade é identidade. Em `switch`, o `case` usa o nome da constante, e quando todos os casos estão cobertos o `switch` expression é exaustivo — nem `default` é preciso.

`values()` e `valueOf()` são métodos estáticos gerados por enum (um para cada tipo declarado), e `values()` devolve um **array novo** a cada chamada.

## Exemplos

Uso básico com comparação e `switch` expression:

```java
enum Status { PENDENTE, APROVADO, REJEITADO }

Status s = Status.APROVADO;
if (s == Status.APROVADO) {            // comparação segura (objeto único)
    System.out.println("liberado");
}

String rotulo = switch (s) {
    case PENDENTE  -> "aguardando";
    case APROVADO  -> "liberado";
    case REJEITADO -> "negado";
};
// rotulo = "liberado" — todas as constantes cobertas, sem default
```

Percorrendo, buscando por nome e usando conjuntos eficientes:

```java
for (Prioridade p : Prioridade.values()) System.out.print(p + " ");
// Saída: BAIXA NORMAL ALTA

Prioridade p = Prioridade.valueOf("ALTA");   // IllegalArgumentException se não existir

EnumSet<Prioridade> lote = EnumSet.of(Prioridade.ALTA, Prioridade.NORMAL);
for (Prioridade x : lote) System.out.print(x + " ");   // Saída: NORMAL ALTA
```

Comportamento específico de cada constante (polimorfismo sem `if`):

```java
enum Acao {
    ABRIR   { void executar() { System.out.println("abrindo"); } },
    FECHAR  { void executar() { System.out.println("fechando"); } };

    abstract void executar();   // cada constante implementa do seu jeito
}

Acao.ABRIR.executar();           // Saída: abrindo
```

## Armadilhas comuns

> [!WARNING] Nunca grave `ordinal()` em banco, arquivo ou API — os números mudam se você inserir uma constante no meio da lista e todos os dados gravados mudam de sentido junto. Guarde o `name()` ou um código estável próprio.

```java
enum Status { PENDENTE, APROVADO, REJEITADO }   // 0, 1, 2
// depois alguém insere CANCELADO no começo → todos os números do banco mudam de significado
String seguro = status.name();                  // "APROVADO" — texto estável
```

**`valueOf` exige o nome exato:**

```java
Prioridade.valueOf("alta");     // IllegalArgumentException — caixa importa
Prioridade.valueOf("ALTA");     // ok
Prioridade.valueOf("Inexistente");  // IllegalArgumentException
```

O texto vindo de tela ou banco precisa bater caractere a caractere com a constante.

**`new` e `extends` não existem:**

```java
new Prioridade();                   // erro: enum classes may not be instantiated
enum Outro extends Prioridade { }   // erro de sintaxe — enum não aceita extends
```

O enum controla a criação: só o compilador instancia, garantindo que exista exatamente um objeto por constante.

## Profundidade

**Enum é uma classe (JLS §8.9):** implicitamente estende `java.lang.Enum<E>` e é `final`. As constantes viram campos `public static final` do próprio tipo (`Status.APROVADO`), o array `$VALUES` guarda a ordem de declaração, e o construtor — escrito ou não — não pode ser público: a instânciação fica restrita ao compilador.

**Identidade, não valor:** `equals`/`hashCode`/`compareTo` são finais em `Enum` e comparam referência/ordinal. Isso torna enum seguro para `==`, chave de mapa e compartilhamento entre threads — e é por isso que sobrescrevê-los nem sequer é possível.

**`ordinal()` só interno:** a posição é usada por `EnumSet`/`EnumMap` (como índice de bit) e por `compareTo` (ordem de declaração). A ordenação é total e estável — funciona em `TreeSet` e `stream().sorted()` —, mas persistir esse número é acoplar seus dados à ordem física do código-fonte.

**`EnumSet` e `EnumMap`:** representam conjuntos e mapas como um bitmap/índice das constantes — mais rápidos e muito mais enxutos que `HashSet`/`HashMap` (`EnumSet.of(A, C)`, `EnumMap.get(Status.X)`). Em código com enums, são sempre a escolha certa para coleções.

**Serialização preserva singletons:** mesmo gravando e lendo o enum de volta, o JVM devolve a **mesma** constante (mecanismo de `readResolve`) — outra razão pela qual enum é confiável como chave e como estado compartilhado.

**Quando não usar enum:** quando o conjunto de valores muda em runtime (configuração, banco, plugin) — enum é fixo em tempo de compilação. Nesses casos, classe com constantes documentadas ou validação de dados é mais honesto do que uma enum que precisa ser recompilada a cada valor novo. Em compensação, para estados e categorias fixos o ganho é direto: tipo fechado, troca exaustiva e nenhum número solto.
