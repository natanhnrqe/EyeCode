---
id: java/jdk/map
title: Map
concept: map
level: beginner
duration: 4
category: COLEÇÕES
members:
- put(): java/jdk/map/put
- putIfAbsent(): java/jdk/map/put-if-absent
- get(): java/jdk/map/get
- getOrDefault(): java/jdk/map/get-or-default
- containsKey(): java/jdk/map/contains-key
- containsValue(): java/jdk/map/contains-value
- remove(): java/jdk/map/remove
- replace(): java/jdk/map/replace
- keySet(): java/jdk/map/key-set
- values(): java/jdk/map/values
- entrySet(): java/jdk/map/entry-set
- forEach(): java/jdk/map/for-each
- computeIfAbsent(): java/jdk/map/compute-if-absent
- computeIfPresent(): java/jdk/map/compute-if-present
- compute(): java/jdk/map/compute
- merge(): java/jdk/map/merge
officialDocs:
  label: Map
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Map.html
related:
  - java/jdk/hash-map
  - java/jdk/linked-hash-map
  - java/jdk/tree-map
  - java/jdk/map-entry
  - java/generics/generics
---
## O que é isso?

`Map` associa cada chave a um valor. As chaves são únicas.

```java
Map<String, Integer> scores = new HashMap<>();
scores.put("Ada", 10);
```

Use `Map` quando o acesso principal for por chave, escolhendo a implementação de acordo com ordem, concorrência e desempenho necessários.
## Operações essenciais

`put` associa uma chave a um valor e substitui o valor anterior quando a chave já existe. `getOrDefault` ajuda a tratar ausência sem inventar uma entrada.

```java
Map<String, Integer> idades = new HashMap<>();
idades.put("Ana", 30);
idades.put("Ana", 31);

System.out.println(idades.get("Ana"));
System.out.println(idades.getOrDefault("Bruno", 0));
```

`containsKey`, `remove`, `keySet`, `values` e `entrySet` cobrem consultas e percursos comuns. Ao precisar de chave e valor juntos, prefira `entrySet`.
