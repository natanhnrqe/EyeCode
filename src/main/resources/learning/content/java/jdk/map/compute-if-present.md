---
id: java/jdk/map/compute-if-present
title: Map.computeIfPresent()
concept: map-compute-if-present
kind: method
sourceMember: computeIfPresent
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/map
related:
  - java/jdk/map
---
## O que ele faz?

Usa computeIfPresent() em Map para realizar a operação indicada pela API Java.

```java
Map<String, Integer> valores = new HashMap<>();
var resultado = valores.computeIfPresent("Ada", (chave, valor) -> valor + 1);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Map. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
