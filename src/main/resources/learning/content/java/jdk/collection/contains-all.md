---
id: java/jdk/collection/contains-all
title: Collection.containsAll()
concept: collection-contains-all
kind: method
sourceMember: containsAll
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collection
related:
  - java/jdk/collection
---
## O que ele faz?

Usa containsAll() em Collection para realizar a operação indicada pela API Java.

```java
Collection<String> valores = new ArrayList<>();
var resultado = valores.containsAll(List.of("Ada"));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collection. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
