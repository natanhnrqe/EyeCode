---
id: java/jdk/map
title: Map
concept: map
level: beginner
duration: 4
category: COLEÇÕES
officialDocs:
  label: Map
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html
related:
  - java/jdk/hash-map
---
## O que é isso?

`Map` associa cada chave a um valor. As chaves são únicas.

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Ada", 10);
```

Use `Map` quando o acesso principal for por chave, escolhendo a implementação de acordo com ordem, concorrência e desempenho necessários.
