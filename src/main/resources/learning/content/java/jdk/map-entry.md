---
id: java/jdk/map-entry
title: Map.Entry
concept: map-entry
level: beginner
duration: 4
category: COLEÇÕES
officialDocs:
  label: Map.Entry
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.Entry.html
related:
  - java/jdk/map
  - java/jdk/hash-map
  - java/jdk/linked-hash-map
  - java/jdk/tree-map
---
## O que é isso?

`Map.Entry<K, V>` representa um par de chave e valor dentro de um mapa. É especialmente útil quando a iteração precisa acessar os dois lados do par.

```java
Map<String, Integer> idades = new HashMap<>();
idades.put("Ana", 30);

for (Map.Entry<String, Integer> entrada : idades.entrySet()) {
    System.out.println(entrada.getKey() + ": " + entrada.getValue());
}
```

`getKey` consulta a chave e `getValue` consulta o valor. A entrada vem de `entrySet`; ela não é uma coleção independente criada para substituir o mapa.

Este card é alcançado por Related. O suporte atual de documentação e fonte para tipos aninhados ainda não registra `Map.Entry` como um alvo semântico direto.
