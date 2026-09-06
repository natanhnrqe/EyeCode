---
id: java/jdk/set/add-all
title: Set.addAll()
concept: set-add-all
kind: method
sourceMember: addAll
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/set
related:
  - java/jdk/set
---
## O que ele faz?

Usa addAll() em Set para realizar a operação indicada pela API Java.

```java
Set<String> valores = new HashSet<>(Set.of("Ada"));
var resultado = valores.addAll(Set.of("Lin"));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Set. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
