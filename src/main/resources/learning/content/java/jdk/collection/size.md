---
id: java/jdk/collection/size
title: Collection.size()
concept: collection-size
kind: method
sourceMember: size
sourceSignature: ()
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collection
related:
  - java/jdk/collection
---
## O que ele faz?

Usa size() em Collection para realizar a operação indicada pela API Java.

```java
Collection<String> valores = new ArrayList<>();
var resultado = valores.size();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collection. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
