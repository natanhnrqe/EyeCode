---
id: java/jdk/collection/add
title: Collection.add()
concept: collection-add
kind: method
sourceMember: add
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collection
related:
  - java/jdk/collection
---
## O que ele faz?

Usa add() em Collection para realizar a operação indicada pela API Java.

```java
Collection<String> valores = new ArrayList<>();
var resultado = valores.add("Ada");
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collection. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
