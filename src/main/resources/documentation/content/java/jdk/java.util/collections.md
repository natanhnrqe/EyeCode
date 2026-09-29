---
id: java/jdk/java.util/collections
title: Collections
type: api
summary: Classe utilitária de coleções do Java, com sort, reverse, shuffle, frequency e as proteções unmodifiable e synchronized explicadas sem mistério.
level: beginner
duration: 7
officialDocs:
  label: API java.util.Collections
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Collections.html
related:
  - java/jdk/java.util/list
  - java/jdk/java.util/comparator
  - java/jdk/java.util/stream
---

> [!INFO] `Collections` é uma **classe estática de utilitários** (nunca instanciável) para operar sobre coleções: ordenar, embaralhar, inverter, contar ocorrências e criar listas protegidas. Já `Collection` (com C maiúsculo) é a interface pai — não confunda os dois.

## Visão geral

Tudo em `Collections` são métodos `static` que recebem uma coleção e devolvem outra (ou alteram a mesma, dependendo do método). É o atalho para operações que você não quer reimplementar a cada projeto.

```java
Collections.sort(nomes);                 // ordena no lugar
Collections.reverse(nomes);              // inverte
Collections.shuffle(nomes);              // embaralha
List<String> vazio = Collections.emptyList();          // lista vazia imutável
List<Integer> dez = Collections.nCopies(10, 0);        // dez zeros
```

Regra de leitura: **nem todo retorno é uma cópia** — alguns métodos devolvem uma *view* ligada à original ou uma proteção só de leitura. Antes de assumir "cópia", veja o que muda na lista de origem.

## Assinaturas

| Método | Retorna | O que faz |
|--------|---------|-----------|
| `sort(List<T>)` | `void` | ordena no lugar (T comparável) |
| `sort(List<T>, Comparator)` | `void` | ordena no lugar com comparador |
| `reverse(List)` | `void` | inverte a ordem |
| `shuffle(List)` | `void` | embaralha (aleatório) |
| `swap(List, int, int)` | `void` | troca duas posições |
| `frequency(Collection, Object)` | `int` | quantas vezes o valor aparece |
| `min(Collection)` / `max(Collection)` | `Object` | menor / maior valor |
| `disjoint(Collection, Collection)` | `boolean` | se não têm elementos em comum |
| `unmodifiableList(List)` | `List` | view que não deixa alterar |
| `synchronizedList(List)` | `List` | versão sincronizada |
| `emptyList()` (genérico) | `List<T>` | lista vazia imutável |
| `singletonList(T)` | `List<T>` | lista com um elemento |
| `nCopies(int, T)` | `List<T>` | n cópias do valor |
| `replaceAll(List, T antigo, T novo)` | `boolean` | troca todas as ocorrências |

## Ordenar e embaralhar

```java
List<Integer> nums = new ArrayList<>(List.of(5, 3, 9, 1));
Collections.sort(nums);
System.out.println(nums);                 // [1, 3, 5, 9]

List<String> times = new ArrayList<>(List.of("Fla", "Bah", "Atl"));
Collections.sort(times, Comparator.reverseOrder());
System.out.println(times);                // [Fla, Bah, Atl]

Collections.shuffle(times);               // ordem aleatória (ex.: [Bah, Fla, Atl])
Collections.swap(times, 0, 1);            // troca as posições 0 e 1
Collections.reverse(times);               // só inverte
```

**`sort` muta a lista recebida** — quem espera uma lista de volta não usa o retorno (é `void`).

## Contar, comparar e testar

```java
List<String> frutas = List.of("uva", "maçã", "uva", "uva");

Collections.frequency(frutas, "uva");      // 3
Collections.min(frutas);                  // "maçã" (ordem natural)
Collections.max(frutas);                  // "uva"
Collections.disjoint(List.of("a", "b"), List.of("c"));   // true — nada em comum
Collections.disjoint(List.of("a"), List.of("a"));        // false
System.out.println(Collections.frequency(frutas, "pera")); // 0
```

`frequency` percorre a coleção toda (O(n)) — para contar repetidamente, transforme em `Map` com `merge` (ver `Map`).

**Armadilha:** `min`/`max` exigem ordem natural; com objetos sem `Comparable`, passe um `Comparator` ou estoura `ClassCastException`.

## Proteger e criar

```java
List<String> origem = new ArrayList<>(List.of("a", "b"));
List<String> somenteLeitura = Collections.unmodifiableList(origem);
origem.add("c");
System.out.println(somenteLeitura);        // [a, b, c] — a view enxerga a mudança!

List<String> copiaProtegida = List.copyOf(origem);   // cópia realmente imutável
List<String> vazia = Collections.emptyList();        // imutável, tipada por inferência
List<Integer> zeros = Collections.nCopies(3, 0);     // [0, 0, 0]
List<String> rotulo = Collections.singletonList("ok");// um elemento só
```

Armadilha: `unmodifiableList` **não copia** — é um escudo sobre a mesma lista; `List.copyOf` sim.

## Armadilhas comuns

> [!WARNING] `Collections.unmodifiableList(lista)` devolve uma *view*: alterações feitas na lista original continuam aparecendo, e você só ganha exceção ao tentar alterar pela view. Para um dado imutável de verdade, use `List.copyOf(lista)`.

**Elementos que não implementam `Comparable`:**

```java
record Pessoa(String nome) {}
List<Pessoa> ps = new ArrayList<>(List.of(new Pessoa("Ana")));
Collections.sort(ps);          // ClassCastException: Pessoa não implementa Comparable
Collections.sort(ps, Comparator.comparing(Pessoa::nome));   // ok: comparador explícito
```

**`Collections` não se instancia:**

```java
new Collections();    // erro de compilação — construtor é privado
Collections.sort(l);  // correto: tudo é static
```

**Proteção e cópia se confundem:**

```java
List<String> a = Collections.unmodifiableList(new ArrayList<>()); // escudo, sem cópia
List<String> b = List.of("x");        // imutável, dados colados no objeto
// escrever em 'a' ou 'b' -> UnsupportedOperationException
```

## Profundidade

**`Collections.sort` vs `list.sort`:** desde o Java 8, `Collections.sort(lista, cmp)` apenas delega para `lista.sort(cmp)` — os dois fazem exatamente a mesma coisa (TimSort). Prefira o método de instância `lista.sort(...)`, mais legível e sem a classe intermediária.

**Ordenação estável:** `sort` usa TimSort, que **preserva a ordem relativa** de elementos considerados iguais — é por isso que ordenar primeiro por nome e depois por cidade resulta em nomes ainda ordenados dentro da mesma cidade (multi-critério sem `thenComparing`).

**`emptyList`/`singletonList` são compartilhadas:** `Collections.emptyList()` devolve uma única instância imutável reutilizável; por isso o tipo precisa ser inferido ou declarado (`List<String> l = Collections.emptyList()`), senão não compila a atribuição.

**`synchronizedList` exige sincronizar a iteração:** a lista é protegida, mas o iterator dela não — o laço deve ficar dentro de `synchronized (lista) { ... }`. Em código novo, prefira `CopyOnWriteArrayList` ou `ConcurrentHashMap` conforme o caso.

**`min`/`max` sem comparador** exigem elementos comparáveis e percorrem a coleção inteira; com `Comparator`, você pode achar o maior por um campo sem ordenar (O(n) em vez de O(n log n)).
