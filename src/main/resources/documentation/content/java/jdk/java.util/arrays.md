---
id: java/jdk/java.util/arrays
title: Arrays
type: api
summary: Utilitários de array do Java, com sort, binarySearch, copyOf, asList e stream, e as armadilhas de array ordenado e de view fixa.
level: beginner
duration: 7
officialDocs:
  label: API java.util.Arrays
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html
related:
  - java/jdk/java.util/list
  - java/jdk/java.util/objects
  - java/jdk/java.util/stream
---

> [!INFO] `Arrays` é a classe estática de apoio a **arrays**: ordenar, buscar, copiar, preencher, comparar e converter para `List`/`Stream`. A maioria dos métodos **altera o próprio array** (devolve `void`) e todos exigem o array ordenado quando fazem busca binária.

## Visão geral

Array é rápido e de tamanho fixo; `Arrays` é o kit que falta para não reimplementar busca e cópia. Pense nele como o `Collections`, só que para `T[]` e arrays primitivos (`int[]`, `double[]`...).

```java
int[] nums = {5, 3, 9};
Arrays.sort(nums);                                  // {3, 5, 9} — in place
List<String> l = Arrays.asList("a", "b");           // view de tamanho fixo
Stream<Integer> s = Arrays.stream(new Integer[]{1, 2});  // Stream<Integer>
```

Os primitivos têm sobrecargas próprias (`sort(int[])`, `binarySearch(int[], int)`) — mais rápidas que as genéricas.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `sort(T[])` / `sort(int[])` | `void` | ordena no lugar (ordem natural) |
| `sort(T[], Comparator)` | `void` | ordena no lugar com comparador |
| `binarySearch(T[], T chave)` | `int` | índice da chave ou `-(ponto de inserção)-1` |
| `asList(T...)` | `List<T>` | view da lista sobre o array |
| `copyOf(T[], int novoTamanho)` | `T[]` | cópia (com novo tamanho) |
| `copyOfRange(T[], int de, int ate)` | `T[]` | cópia de um intervalo |
| `fill(T[], T valor)` | `void` | preenche tudo com o valor |
| `setAll(T[], IntFunction)` | `void` | preenche pela função do índice |
| `equals(T[], T[])` | `boolean` | comparação por elemento |
| `deepEquals(Object[], Object[])` | `boolean` | compara aprofundando arrays aninhados |
| `mismatch(T[], T[])` (Java 9) | `int` | primeira posição diferente ou `-1` |
| `hashCode(T[])` | `int` | hash dos elementos |
| `toString(T[])` | `String` | texto `[a, b, c]` |
| `stream(T[])` / `stream(int[])` | `Stream` / `IntStream` | stream sobre os elementos |

## Ordenar e buscar

```java
int[] nums = {5, 3, 9, 1};
Arrays.sort(nums);
System.out.println(Arrays.toString(nums));        // [1, 3, 5, 9]
System.out.println(Arrays.binarySearch(nums, 5)); // 2 (índice real)
System.out.println(Arrays.binarySearch(nums, 4)); // -3 — não achou: -(ponto de inserção)-1

List<String> times = new ArrayList<>(List.of("Fla", "Atl", "Bah"));
times.sort(Comparator.naturalOrder());
System.out.println(times);                        // [Atl, Bah, Fla]

String[] nomes = {"bia", "ana"};
Arrays.sort(nomes, Comparator.reverseOrder());
System.out.println(Arrays.toString(nomes));       // [bia, ana]
```

Armadilha: `binarySearch` **só é válido em array ordenado** pelo mesmo critério — num array desordenado ele devolve um índice qualquer, sem sinal de erro.

## Copiar e preencher

```java
int[] origem = {1, 2, 3};
int[] copia = Arrays.copyOf(origem, 5);          // {1, 2, 3, 0, 0} — completa com 0
int[] trecho = Arrays.copyOfRange(origem, 1, 3); // {2, 3} — intervalo [de, ate)
int[] cheio = new int[3];
Arrays.fill(cheio, 7);                            // {7, 7, 7}
Arrays.setAll(cheio, i -> i * 2);                 // {0, 2, 4}
System.out.println(Arrays.toString(copia) + " | " + Arrays.toString(trecho));
// [1, 2, 3, 0, 0] | [2, 3]

System.out.println(Arrays.equals(new int[]{1, 2}, new int[]{1, 2}));   // true
System.out.println(Arrays.toString(new int[]{1, 2}));                  // [1, 2]
```

`copyOf` cria um **array novo** (cópia de verdade); o original não muda.

**Armadilha:** ao crescer, `copyOf` preenche com `0`/`null` — é valor padrão, não dado novo.

## Converter entre array e coleção

```java
String[] arr = {"a", "b", "c"};

List<String> l1 = Arrays.asList(arr);     // view: set ok, add/remove lançam exceção
l1.set(0, "z");                           // ok — altera arr[0]
// l1.add("d");                           // UnsupportedOperationException

List<String> l2 = List.of(arr);           // imutável, cópia dos dados
List<String> l3 = new ArrayList<>(List.of(arr));   // mutável de verdade

arr = l3.toArray(String[]::new);          // volta para array do tipo certo
Stream<String> s = Arrays.stream(arr);    // stream para processar
int[] prim = {1, 2, 3};
IntStream is = Arrays.stream(prim);       // primitivo vira IntStream (sem boxing)
System.out.println(is.sum());             // 6
```

**Armadilha:** `Arrays.asList(arr)` é uma **view** — `l1.set(0, "z")` também troca `arr[0]`.

## Armadilhas comuns

> [!WARNING] `Arrays.asList(array)` devolve uma lista de **tamanho fixo, ligada ao array** — `add`/`remove` lançam `UnsupportedOperationException` e `set` altera o array original. Para uma coleção que cresce, use `new ArrayList<>(...)`.

**Varargs engole arrays de primitivos:**

```java
int[] nums = {1, 2, 3};
List<int[]> l = Arrays.asList(nums);   // List com UM elemento (um int[]), não 3!
List<Integer> ok = Arrays.asList(1, 2, 3);   // o que você provavelmente queria
System.out.println(l.size());          // 1
```

**Comparar arrays com `==`:**

```java
int[] a = {1, 2};
int[] b = {1, 2};
System.out.println(a == b);                 // false — compara endereço
System.out.println(Arrays.equals(a, b));    // true — compara conteúdo
System.out.println(Arrays.deepEquals(new int[][]{{1}}, new int[][]{{1}})); // true
```

**Ordenar muda o original:**

```java
List<Integer> original = new ArrayList<>(List.of(3, 1, 2));
Integer[] arr = original.toArray(Integer[]::new);
Arrays.sort(arr);                     // só 'arr' foi ordenado
System.out.println(original);         // [3, 1, 2] (se era cópia independente)
```

## Profundidade

**Algoritmos e complexidade:** arrays primitivos usam quicksort de pivô duplo (O(n log n) médio); objetos genéricos usam **TimSort**, estável, também O(n log n). `binarySearch` é O(log n) e depende do contrato de ordenação — inclusive do `Comparator` usado na hora de ordenar.

**`equals` profundo:** `Arrays.equals` compara elemento a elemento com `equals()` dos elementos — para `int[][]` ele compara as referências internas. `deepEquals`, `hashCode` e `toString` têm variantes "deep" que desmontam arrays aninhados recursivamente.

**`asList` é view, não cópia:** o método é varargs, então recebe um array e devolve `List` que aponta para ele. Por isso `set` reflete no array e `add` é proibido — mudar o tamanho exigiria redimensionar o array de trás.

**Boxing:** `Arrays.stream(int[])` devolve `IntStream` (primitivo, sem `Integer` criado); `Arrays.stream(Integer[])` devolve `Stream<Integer>`. Para somas e contagens em volumes grandes, prefira a versão primitiva.

**Genéricos e arrays (JLS §10.5):** arrays são **covariantes** (`Integer[]` é um `Object[]`), mas a passagem por varargs gera avisos de *heap pollution* — é por isso que a API usa `@SafeVarargs` em métodos como `List.of`.
