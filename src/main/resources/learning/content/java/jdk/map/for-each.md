---
id: java/jdk/map/for-each
title: Map.forEach()
concept: map-for-each
kind: method
sourceMember: forEach
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/map
related:
  - java/jdk/map
---
## O que ele faz?

Usa forEach() em Map para realizar a operação indicada pela API Java.

```java
Map<String, Integer> valores = new HashMap<>();
valores.forEach((chave, valor) -> System.out.println(chave));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Map. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
