---
id: java/jdk/java.util/stream
title: Stream
type: api
summary: Streams do Java para processar coleções de forma declarativa, com map, filter, collect e a regra de ser preguiçoso e de uso único.
level: beginner
duration: 9
officialDocs:
  label: API java.util.stream.Stream
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/stream/Stream.html
related:
  - java/jdk/java.util/list
  - java/jdk/java.util/collections
  - java/jdk/java.util/optional
---

> [!INFO] `Stream` é um **fluxo preguiçoso** de elementos: as operações intermediárias (`map`, `filter`) só montam o plano e **nada executa** até uma operação terminal (`collect`, `forEach`, `reduce`). E um stream é de **uso único** — consumiu, não reutiliza.

## Visão geral

Stream traduz laços aninhados em uma cadeia legível: transformar, filtrar, ordenar, juntar. A coleção de origem **nunca muda** — o stream só a lê e devolve um resultado novo.

```java
List<String> nomes = List.of("ana", "bia", "caio");

List<String> longos = nomes.stream()
        .filter(n -> n.length() > 3)
        .map(String::toUpperCase)
        .toList();                       // [CAIO]

long total = nomes.stream().count();     // 3
```

Fontes comuns: `colecao.stream()`, `Stream.of(...)`, `Arrays.stream(array)`, `String.lines()`, `Map.entrySet().stream()`.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `filter(Predicate)` | `Stream` | mantém quem satisfaz a condição (intermediária) |
| `map(Function)` | `Stream` | transforma cada elemento (intermediária) |
| `flatMap(Function)` | `Stream` | transforma e achata (intermediária) |
| `distinct()` | `Stream` | remove duplicados (intermediária) |
| `sorted()` / `sorted(Comparator)` | `Stream` | ordena (intermediária, estado) |
| `limit(long)` / `skip(long)` | `Stream` | corta o fluxo (intermediária) |
| `peek(Consumer)` | `Stream` | observa sem alterar (intermediária) |
| `forEach(Consumer)` | `void` | consome cada elemento (terminal) |
| `collect(Collector)` | `<R> R` | junta em lista/mapa/string (terminal) |
| `reduce(...)` | `Optional` / valor | combina tudo num resultado (terminal) |
| `count()` | `long` | quantidade (terminal) |
| `findFirst()` / `findAny()` | `Optional<E>` | primeiro/qualquer (terminal) |
| `anyMatch` / `allMatch` / `noneMatch` | `boolean` | teste global (terminal) |
| `toList()` / `toSet()` | `List` / `Set` | atalhos de coleção (terminal) |

## Transformar e filtrar

```java
List<String> produtos = List.of("cafe", "cha", "suco");
List<String> maiusculas = produtos.stream()
        .filter(p -> p.length() > 3)
        .map(String::toUpperCase)
        .toList();
System.out.println(maiusculas);          // [CAFE, SUCO]

List<Integer> nums = List.of(1, 2, 3, 4, 5);
List<Integer> pares2 = nums.stream()
        .filter(n -> n % 2 == 0)
        .map(n -> n * 10)
        .toList();
System.out.println(pares2);              // [20, 40]
```

**Ordem importa:** `filter` antes de `map` evita transformar o que seria descartado; `map` antes de `filter` só trabalha à toa.

## Reduzir e colecionar

```java
List<Integer> nums = List.of(1, 2, 3, 4);
int soma = nums.stream().reduce(0, Integer::sum);      // 10
Optional<Integer> maior = nums.stream().max(Integer::compare);  // Optional[4]

List<String> juntas = nums.stream().map(String::valueOf).collect(Collectors.joining(", "));
System.out.println(juntas);              // 1, 2, 3, 4

Map<Boolean, List<Integer>> paridade = nums.stream()
        .collect(Collectors.partitioningBy(n -> n % 2 == 0));
System.out.println(paridade.get(true));  // [2, 4]

Map<String, Long> contagem = List.of("a", "b", "a").stream()
        .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
System.out.println(contagem);            // {a=2, b=1}
```

**Armadilha:** `reduce` sem valor inicial devolve `Optional` — stream vazio dá `Optional.empty()`, trate com `orElse`.

## Ordenar e limitar

```java
List<String> times = List.of("Flamengo", "Bahia", "Atlético");

List<String> alfabeto = times.stream().sorted().toList();
System.out.println(alfabeto);            // [Atlético, Bahia, Flamengo]

List<String> porTamanho = times.stream()
        .sorted(Comparator.comparingInt(String::length))
        .toList();
System.out.println(porTamanho);          // [Bahia, Flamengo, Atlético] — empate preserva a ordem original

List<String> doisPrimeiros = times.stream().sorted().limit(2).toList();
System.out.println(doisPrimeiros);       // [Atlético, Bahia]
boolean algumLongo = times.stream().anyMatch(t -> t.length() > 6);   // true
// anyMatch é de corte: para em encontrar o primeiro true
```

Armadilha: `sorted()` com stream grande é O(n log n) **e** guarda tudo em memória — `limit` depois de `sorted` não economiza o trabalho de ordenar.

## Armadilhas comuns

> [!WARNING] Um `Stream` só pode ser consumido **uma vez**: reutilizar a variável lança `IllegalStateException: Stream has already been operated or closed`. Guarde a fonte (`List`), não o stream — ou crie um novo com `.stream()` a cada uso.

**Nada executa sem operação terminal:**

```java
Stream<String> s = List.of("a", "b").stream().map(x -> {
    System.out.println("processando " + x);   // NÃO imprime nada aqui
    return x;
});
// o println só aparece quando existe .toList(), .forEach(), .count() etc.
List<String> ok = s.toList();          // agora sim: processando a / processando b
```

**Stream não modifica a coleção de origem:**

```java
List<Integer> nums = new ArrayList<>(List.of(3, 1, 2));
List<Integer> ordenado = nums.stream().sorted().toList();
System.out.println(ordenado);          // [1, 2, 3]
System.out.println(nums);              // [3, 1, 2] — origem intacta
nums.sort(null);                       // este sim altera a lista
```

**`forEach` com efeito colateral em paralelo:** gravar em listas normais dentro de `parallelStream().forEach(...)` corrompe dados — use `collect` ou `forEachOrdered` quando a ordem importa.

## Profundidade

**Avaliação preguiçosa (lazy):** o pipeline monta uma sequência de estágios (spliterator + operações) e só percorre os dados na terminal. É o que permite `filter(...).findFirst()` parar no primeiro resultado — `findFirst`, `anyMatch`, `allMatch` e `limit` são **operações de corte**.

**Streams não têm estado entre chamadas:** `map`/`filter` são *stateless*; `sorted`, `distinct`, `limit` e `skip` são *stateful* (precisam de memória para o resultado). `sorted` força o stream inteiro a ser materializado antes de passar adiante.

**`Collectors` e paralelismo:** `collect` usa um `Collector` com *supplier/accumulator/combiner* — o mesmo código funciona em `parallelStream()` porque o combiner junta resultados parciais. Já `reduce` exige associatividade para ser paralelo seguro.

**Encounter order:** streams de lista preservam a ordem de encontro; `HashSet` não. `forEachOrdered` mantém a ordem no paralelo (ao custo de serializar o consumo), `forEach` não garante nada.

**Imutabilidade por design:** pipeline nunca escreve na fonte — por isso stream é a forma idiomática de "ler, transformar e devolver algo novo", enquanto `list.sort`/`removeIf` são a forma de alterar no lugar.
