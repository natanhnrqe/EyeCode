---
id: java/jdk/linked-hash-map
title: LinkedHashMap
concept: linked-hash-map
level: beginner
duration: 5
category: COLEÇÕES
officialDocs:
  label: LinkedHashMap
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/LinkedHashMap.html
related:
  - java/jdk/map
  - java/jdk/hash-map
  - java/jdk/tree-map
  - java/collections/choosing
---
## O que é isso?

`LinkedHashMap` é um `Map` baseado em hashing que mantém a ordem de encontro das entradas em seu modo padrão.

```java
Map<String, Integer> idades = new LinkedHashMap<>();
idades.put("Ana", 30);
idades.put("Bruno", 25);

for (Map.Entry<String, Integer> entrada : idades.entrySet()) {
    System.out.println(entrada.getKey() + ": " + entrada.getValue());
}
```

Esse comportamento é útil quando a iteração deve seguir a ordem de inserção. A implementação também possui modo de ordem por acesso, mas ele deve ser escolhido conscientemente e não é a configuração padrão.
