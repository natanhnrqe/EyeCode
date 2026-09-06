---
id: java/jdk/set/remove
title: Set.remove()
concept: set-remove
kind: method
sourceMember: remove
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/set
related:
  - java/jdk/set
---
## O que ele faz?

Usa remove() em Set para realizar a operação indicada pela API Java.

```java
Set<String> valores = new HashSet<>(Set.of("Ada"));
var resultado = valores.remove("Ada");
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Set. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
