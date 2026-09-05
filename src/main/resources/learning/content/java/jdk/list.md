---
id: java/jdk/list
title: List
concept: list
level: beginner
duration: 4
category: COLEÇÕES
officialDocs:
  label: List
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html
related:
  - java/jdk/array-list
  - java/jdk/collection
  - java/jdk/linked-list
  - java/jdk/iterable
  - java/generics/generics
---
## O que é isso?

`List` é uma coleção ordenada que pode conter valores repetidos.

```java
List<String> names = new ArrayList<>();
names.add("Ada");
```

Use `List` quando a ordem dos elementos fizer parte do contrato e quando a implementação concreta puder variar.
## Operações e contrato

`get`, `set`, `add` e `remove` usam índices começando em zero. A interface permite duplicatas e não obriga uma implementação específica.

```java
List<String> nomes = new ArrayList<>();
nomes.add("Ana");
nomes.add("Bruno");
nomes.add("Ana");

System.out.println(nomes.get(1));
nomes.set(1, "Carla");
nomes.remove(0);
```

Use `List` na declaração quando o restante do código só precisa do contrato de sequência. Escolha `ArrayList` ou `LinkedList` quando a representação concreta for realmente relevante.
