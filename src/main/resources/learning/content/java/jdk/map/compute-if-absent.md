---
id: java/jdk/map/compute-if-absent
title: Map.computeIfAbsent()
concept: map-compute-if-absent
kind: method
sourceMember: computeIfAbsent
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/map
related:
  - java/jdk/map
---
## O que ele faz?

Calcula e armazena um valor somente quando a chave ainda não possui valor.

```java
Map<String, Integer> tamanhos = new HashMap<>();
tamanhos.computeIfAbsent("Ada", String::length);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Map. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
