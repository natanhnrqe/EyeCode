---
id: java/jdk/list/get
title: List.get()
concept: list-get
kind: method
sourceMember: get
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/list
related:
  - java/jdk/list
---
## O que ele faz?

Usa get() em List para realizar a operação indicada pela API Java.

```java
List<String> valores = new ArrayList<>(List.of("Ada"));
var resultado = valores.get(0);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de List. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
