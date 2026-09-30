---
id: java/jdk/java.util/function
title: Function, Predicate e cia.
type: api
summary: As interfaces funcionais centrais do JDK — Function, Predicate, Consumer e Supplier, composição com andThen/compose e as variantes primitivas sem autoboxing.
level: intermediate
duration: 8
officialDocs:
  label: API java.util.function (pacote)
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/function/package-summary.html
related:
  - java/jdk/fundamentos/lambdas
  - java/jdk/java.util/stream
  - java/jdk/java.util/optional
---

> [!INFO] `java.util.function` é o **vocabulário padrão dos lambdas**: `Function` (entra T, sai R), `Predicate` (entra T, sai `boolean`), `Consumer` (entra T, não sai nada) e `Supplier` (não entra nada, sai T). APIs como `Stream` e `Optional` falam essa língua — e composição (`andThen`, `compose`, `and`, `or`) liga as peças.

## Visão geral

Todo lambda precisa de um **tipo alvo** — uma interface com um único método abstrato. O pacote `java.util.function` padroniza essas interfaces para que `stream.map`, `optional.filter` e a sua própria API usem os mesmos nomes, em vez de cada um inventar `MeuCallback`:

```java
Function<String, Integer> tamanho = String::length;
tamanho.apply("EyeCode");   // 7
```

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `Function<T,R>.apply(T t)` | `R` | transforma a entrada no resultado |
| `Function.andThen(Function after)` | `Function<T,V>` | aplica esta e **depois** a outra |
| `Function.compose(Function before)` | `Function<V,R>` | aplica a outra **antes** desta |
| `Function.identity()` | `Function<T,T>` | devolve a própria entrada |
| `Predicate<T>.test(T t)` | `boolean` | avalia a condição |
| `Predicate.and/or/negate(Predicate)` | `Predicate<T>` | combina condições |
| `Consumer<T>.accept(T t)` | `void` | consome o valor (efeito colateral) |
| `Supplier<T>.get()` | `T` | produz um valor sem entrada |
| `BiFunction<T,U,R>.apply(T t, U u)` | `R` | recebe duas entradas |
| `UnaryOperator<T>` | `Function<T,T>` | entra e sai o mesmo tipo |
| `BinaryOperator<T>` | `BiFunction<T,T,T>` | reduz dois T em um T |
| `IntPredicate`, `IntFunction<R>`, `ToIntFunction<T>`... | — | variantes primitivas (sem autoboxing) |

## As quatro interfaces base

```java
Function<String, Integer> tamanho = String::length;
Predicate<String> vazio = String::isBlank;
Consumer<String> log = s -> System.out.println("[log] " + s);
Supplier<ArrayList<String>> novo = ArrayList::new;

tamanho.apply("EyeCode");  // 7
vazio.test("");            // true
log.accept("iniciado");    // imprime no console
novo.get();                // uma lista recém-criada
```

| Interface | Forma | Use quando |
|-----------|-------|------------|
| `Function<T,R>` | T → R | transforma o valor |
| `Predicate<T>` | T → `boolean` | filtra, testa condição |
| `Consumer<T>` | T → `void` | executa efeito: log, salvar, notificar |
| `Supplier<T>` | () → T | fabrica/fornece um valor |
| `UnaryOperator<T>` | T → T | transforma dentro do mesmo tipo (`map` de lista) |
| `BinaryOperator<T>` | (T, T) → T | reduz dois valores em um (`reduce`) |

Method references (`String::length`, `ArrayList::new`) são lambdas enxutos — o tipo alvo continua sendo uma dessas interfaces.

## Composição de Function

`andThen` e `compose` montam pipelines sem criar variáveis intermediárias — a diferença é só a ordem:

```java
Function<Integer, Integer> dobra = x -> x * 2;
Function<Integer, Integer> somaDez = x -> x + 10;

dobra.andThen(somaDez).apply(5);   // 20 — 5*2=10, depois 10+10
dobra.compose(somaDez).apply(5);   // 30 — 5+10=15, depois 15*2
Function.identity().apply("x");    // "x" — útil como padrão em mapas de estratégia

// em pipelines reais, o map encadeia a mesma ideia:
List<String> nomes = List.of("ana", "bruno");
nomes.stream()
     .map(String::toUpperCase)   // Function
     .map(s -> s + "!")          // Function
     .toList();                   // ["ANA!", "BRUNO!"]
```

Lembre assim: `andThen` = "eu, **depois** você"; `compose` = "você, **antes** de mim".

## Combinação de Predicate e variantes primitivas

```java
Predicate<String> naoVazio = s -> !s.isBlank();
Predicate<String> curto = s -> s.length() <= 10;

naoVazio.and(curto).test("ok");    // true — as duas condições
naoVazio.or(curto).test("");       // true — basta uma condição ("curto" casa "")
naoVazio.negate().test("");       // true — a negação

// variantes primitivas: sem autoboxing em laços quentes
IntPredicate positivo = i -> i > 0;
ToIntFunction<String> comprimento = String::length;
positivo.test(3);                   // true — int direto, sem Integer
comprimento.applyAsInt("EyeCode");  // 7
```

## Armadilhas comuns

> [!WARNING] Lambda **precisa de tipo alvo**: `var f = x -> x + 1;` não compila — o compilador só aceita o lambda porque sabe qual interface funcional ele implementa. E as interfaces deste pacote **não declaram exceções checked** (`apply` não lança): lambdas que precisam lançar `IOException` não encaixam nelas sem try/catch interno.

**Ordem de composição confunde:**

```java
dobra.andThen(somaDez).apply(5);   // 20 — dobra primeiro
dobra.compose(somaDez).apply(5);   // 30 — somaDez primeiro
```

Trocar uma pela outra compila normal e muda o resultado — nomeie bem as funções ou comente a ordem esperada.

**Autoboxing em laço quente:** `Predicate<Integer>` empacota cada valor em `Integer`; em milhões de iterações isso vira pressão de GC. Prefira `IntPredicate`, `IntUnaryOperator` e companhia.

**`Consumer` não devolve valor:** se o pipeline precisa do resultado, não é `Consumer` — é `Function`. Consumer é para efeitos colaterais (log, métrica, persistência fire-and-forget).

**Duas entradas é o limite padrão:** `BiFunction` cobre dois argumentos; para três ou mais, declare a sua própria `@FunctionalInterface`.

## Profundidade

O pacote chegou com os lambdas (JSR 335, Java 8): todo lambda e method reference precisa de um tipo alvo, e o pacote padroniza as ~40 interfaces mais comuns para que `Stream`, `Optional`, `CompletableFuture` e as APIs novas interoperate sem inventar tipos a cada vez. A nomenclatura é sistemática: prefixo `Bi` (duas entradas), prefixos primitivos `Int`/`Long`/`Double`, formas `To…Function` (retorno primitivo) e sufixo `…Operator` (entra e sai o mesmo tipo). `@FunctionalInterface` é só documentação e verificação — o que importa é ter exatamente um método abstrato.
