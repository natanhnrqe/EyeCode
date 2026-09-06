---
id: java/jdk/map/remove
title: Map.remove()
concept: map-remove
kind: method
sourceMember: remove
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/map
related:
  - java/jdk/map
---
## O que ele faz?

Usa remove() em Map para realizar a operação indicada pela API Java.

```java
Map<String, Integer> valores = new HashMap<>();
var resultado = valores.remove("Ada");
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Map. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
