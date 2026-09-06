---
id: java/jdk/comparator
title: Comparator
concept: comparator
level: beginner
duration: 6
members:
- compare(): java/jdk/comparator/compare
- reversed(): java/jdk/comparator/reversed
- thenComparing(): java/jdk/comparator/then-comparing
- comparing(): java/jdk/comparator/comparing
- naturalOrder(): java/jdk/comparator/natural-order
- reverseOrder(): java/jdk/comparator/reverse-order
category: COLEÇÕES
officialDocs:
  label: Comparator
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Comparator.html
related:
  - java/jdk/comparable
  - java/jdk/tree-set
  - java/jdk/tree-map
  - java/jdk/priority-queue
---
## O que é isso?

`Comparable<T>` define a ordenação natural dentro do próprio tipo. `Comparator<T>` representa uma estratégia externa, que pode ser escolhida conforme a operação.

```java
Comparator<String> porTamanho = (a, b) ->
        Integer.compare(a.length(), b.length());

List<String> nomes = new ArrayList<>();
nomes.add("Ana");
nomes.add("Beatriz");
nomes.sort(porTamanho);
```

Separar a estratégia do tipo permite ordenar o mesmo conjunto de maneiras diferentes sem alterar a classe dos elementos. `TreeSet`, `TreeMap` e `PriorityQueue` também podem receber um comparador.
