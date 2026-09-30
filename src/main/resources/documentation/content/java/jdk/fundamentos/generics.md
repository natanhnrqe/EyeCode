---
id: java/jdk/fundamentos/generics
title: Generics
type: concept
summary: Tipos parametrizados de verdade — List<String> sem casts, bounded types com extends, wildcards ? extends e ? super, e o type erasure que apaga tudo em runtime.
level: intermediate
duration: 9
officialDocs:
  label: JLS §4.5 — Type Arguments and Wildcards
  url: https://docs.oracle.com/javase/specs/jls/se21/html/jls-4.html#jls-4.5
related:
  - java/jdk/java.util/list
  - java/jdk/java.util/map
  - java/jdk/java.lang/comparable
---

> [!INFO] Generics deixa você **parametrizar tipos**: `List<String>` só aceita `String`, `Caixa<Pedido>` guarda pedidos sem cast. O compilador verifica os usos e insere os casts por você — o erro que explodia em runtime (`ClassCastException`) vira erro de **compilação**.

## Por que existe

Antes do Java 5, coleção guardava `Object`. Colocar era fácil; ler exigia cast — `String s = (String) lista.get(0);` — e se alguém tivesse colocado um `Integer` ali, o programa quebrava com `ClassCastException` longe de onde o bug nasceu.

Generics move esse erro para a compilação: `List<String>` aceita só `String`, devolve `String`, e o compilador recusa tudo o que não bate. Ainda melhor: a **mesma** classe serve para todos os tipos — `ArrayList<String>`, `ArrayList<Integer>` — escrita uma única vez.

## Anatomia da sintaxe

Declarar um tipo genérico e usá-lo:

```java
class Caixa<T> {                       // T = parâmetro de tipo
    private T conteudo;
    void guardar(T item) { conteudo = item; }
    T pegar() { return conteudo; }
}

Caixa<String> caixa = new Caixa<>();   // String substitui T (o diamante infere)
```

| Peça | Nome | Exemplo |
|------|------|---------|
| `<T>` | parâmetro de tipo (na declaração) | `class Caixa<T>` |
| `Caixa<String>` | tipo parametrizado (no uso) | `new Caixa<String>()` |
| `<T extends Comparable<T>>` | bounded type (T limitado) | só entra subtipo de `Comparable` |
| `<?>`, `<? extends X>`, `<? super X>` | wildcards (uso flexível) | `List<? extends Number>` |
| `<K, V>` | vários parâmetros | `interface Map<K, V>` |

## Como funciona

Quando o compilador encontra `Caixa<String>`, ele **verifica** cada uso (`guardar(42)` não compila) e **insere** os casts nas leituras (`pegar()` já devolve `String`). Depois disso, apaga: é o **type erasure** — `T` vira o bound declarado (ou `Object`, se não houver) e no bytecode só existe `Caixa`.

O erasure tem três consequências diretas:

1. Em runtime não existe `List<String>` — só `List`; `instanceof List<String>` nem compila;
2. `new T()` e `new T[10]` são proibidos — o tipo não existe quando o código roda;
3. Não dá para sobrecarregar `f(List<String>)` e `f(List<Integer>)` — depois de apagar, as assinaturas são iguais.

Bounds estreitam o parâmetro: `<T extends Comparable<T>>` garante que qualquer `T` tem `compareTo`, então o corpo do método pode chamá-lo. Wildcards flexibilizam o uso: `? extends` aceita subtipos para **ler**; `? super` aceita supertipos para **gravar**.

## Exemplos

Método genérico — `T` é declarado antes do retorno e deduzido na chamada:

```java
static <T> T primeiro(List<T> lista) {
    return lista.get(0);
}

String s = primeiro(List.of("a", "b"));   // T vira String
Integer n = primeiro(List.of(1, 2));      // T vira Integer
```

Bounded type — só entra o que é comparável, e por isso `compareTo` compila:

```java
static <T extends Comparable<T>> T maior(List<T> itens) {
    T m = itens.get(0);
    for (T item : itens) {
        if (item.compareTo(m) > 0) m = item;   // liberado pelo bound
    }
    return m;
}

maior(List.of(3, 7, 2));    // 7
maior(List.of("b", "a"));   // "b"
```

Wildcards — ler de qualquer subtipo, gravar em qualquer supertipo:

```java
static void copiar(List<? extends Number> origem, List<? super Number> destino) {
    for (Number n : origem) destino.add(n);
}

copiar(List.of(1, 2), new ArrayList<Number>());   // ok
copiar(List.of(1, 2), new ArrayList<Object>());   // ok — Object aceita Number
copiar(List.of(1, 2), new ArrayList<Double>());   // erro: Double não é supertipo de Number
```

> [!TIP] Mnemônico clássico: **PECS** — *Producer `extends`, Consumer `super`*. Se a variável fornece dados para você ler, use `? extends`; se recebe dados que você grava, use `? super`.

## Armadilhas

> [!WARNING] Duas assinaturas que diferem **só no genérico** colidem: depois do erasure, `processar(List<String>)` e `processar(List<Integer>)` são o mesmo método. O compilador recusa com *name clash* — não é warning, é erro.

**Criar array de tipo parametrizado:**

```java
List<String>[] lote = new ArrayList<String>[10];   // erro: generic array creation
List<List<String>> lotes = new ArrayList<>();      // alternativa: lista de listas
```

**`instanceof` com genérico não existe:**

```java
Object o = List.of("a");
if (o instanceof List<String> lista) { }   // erro: tipo não verificável em runtime
if (o instanceof List<?> lista) { }        // ok — wildcard ilimitado é permitido
```

**Tipo bruto (`raw type`) reabre a porta do cast:** usar `List` sem `<>` desliga a checagem — o compilador aceita com warning e o `ClassCastException` volta a aparecer longe do bug. Sempre parametrize.

**Membro `static` não enxerga o `T` da classe:** o parâmetro pertence às instâncias; membros compartilhados pela classe não podem depender dele. A saída é um método genérico próprio: `static <T> Caixa<T> vazia()`.

## Profundidade

**Erasure (JLS §4.6):** a compilação substitui `T` pelo bound (ou `Object`) e gera *bridge methods* para manter o polimorfismo onde precisar. É por isso que `List.class` existe e `List<String>.class` não — e por que `new ArrayList<String>().getClass()` devolve o mesmo `ArrayList` de sempre.

**Tipos reificáveis:** só tipos verificáveis em runtime podem aparecer em `instanceof`, arrays genéricos e varargs — `List<?>` sim, `List<String>` não. Por isso varargs genéricos (`List<String>...`) pedem `@SafeVarargs` quando você mesmo assume o risco.

**Variância por uso:** Java é invariante por padrão — `List<Integer>` **não é** `List<Number>`, senão você gravaria um `Double` numa lista de `Integer`. `? extends` e `? super` criam os dois sentidos de flexibilidade **no ponto do uso**, sem abrir mão da segurança.

**Genéricos não aceitam primitivos:** `List<int>` não compila — consequência direta do erasure (`T` vira `Object`). Use `List<Integer>`; o autoboxing faz a ponte.

**Onde a API usa isso:** toda a Collections Framework é genérica (`List<E>`, `Map<K,V>`), e o bound com wildcard aparece em `Collections.sort` como `<T extends Comparable<? super T>>` — o `super` é o que permite ordenar uma `List<Aluno>` com um comparador de `Pessoa`.
