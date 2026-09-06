---
id: java/jdk/collection/retain-all
title: Collection.retainAll()
concept: collection-retain-all
kind: method
sourceMember: retainAll
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collection
related:
  - java/jdk/collection
---
## O que ele faz?

Usa retainAll() em Collection para realizar a operação indicada pela API Java.

```java
Collection<String> valores = new ArrayList<>();
var resultado = valores.retainAll(List.of("Ada"));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collection. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
