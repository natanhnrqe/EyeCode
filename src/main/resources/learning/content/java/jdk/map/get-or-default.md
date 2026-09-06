---
id: java/jdk/map/get-or-default
title: Map.getOrDefault()
concept: map-get-or-default
kind: method
sourceMember: getOrDefault
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/map
related:
  - java/jdk/map
---
## O que ele faz?

Usa getOrDefault() em Map para realizar a operação indicada pela API Java.

```java
Map<String, Integer> valores = new HashMap<>();
var resultado = valores.getOrDefault("Lin", 0);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Map. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
