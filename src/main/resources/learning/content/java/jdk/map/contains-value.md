---
id: java/jdk/map/contains-value
title: Map.containsValue()
concept: map-contains-value
kind: method
sourceMember: containsValue
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/map
related:
  - java/jdk/map
---
## O que ele faz?

Usa containsValue() em Map para realizar a operação indicada pela API Java.

```java
Map<String, Integer> valores = new HashMap<>();
var resultado = valores.containsValue(1);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Map. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
