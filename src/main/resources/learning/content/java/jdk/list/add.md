---
id: java/jdk/list/add
title: List.add()
concept: list-add
kind: method
sourceMember: add
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/list
related:
  - java/jdk/list
---
## O que ele faz?

Usa add() em List para realizar a operação indicada pela API Java.

```java
List<String> valores = new ArrayList<>(List.of("Ada"));
var resultado = valores.add("Ada");
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de List. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
