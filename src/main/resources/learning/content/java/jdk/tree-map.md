---
id: java/jdk/tree-map
title: TreeMap
concept: tree-map
level: beginner
duration: 6
category: COLEÇÕES
officialDocs:
  label: TreeMap
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/TreeMap.html
related:
  - java/jdk/map
  - java/jdk/linked-hash-map
  - java/jdk/comparator
  - java/collections/choosing
---
## O que é isso?

`TreeMap` é um `Map` navegável que mantém as chaves em ordem natural ou na ordem definida por um `Comparator`.

```java
Map<String, Integer> idades = new TreeMap<>();
idades.put("Bruno", 25);
idades.put("Ana", 30);

System.out.println(idades.firstKey());
System.out.println(idades.lastKey());
```

As chaves precisam ser compatíveis com a ordenação usada. O tipo é adequado quando a ordem das chaves faz parte do problema, não apenas quando é necessário armazenar pares.
