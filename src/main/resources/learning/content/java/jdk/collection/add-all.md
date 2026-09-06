---
id: java/jdk/collection/add-all
title: Collection.addAll()
concept: collection-add-all
kind: method
sourceMember: addAll
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collection
related:
  - java/jdk/collection
---
## O que ele faz?

Usa addAll() em Collection para realizar a operação indicada pela API Java.

```java
Collection<String> valores = new ArrayList<>();
var resultado = valores.addAll(List.of("Ada"));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collection. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
