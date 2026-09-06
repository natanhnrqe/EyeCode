---
id: java/jdk/arrays/deep-to-string
title: Arrays.deepToString()
concept: arrays-deep-to-string
kind: method
sourceMember: deepToString
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/arrays
related:
  - java/jdk/arrays
---
## O que ele faz?

Usa deepToString() em Arrays para realizar a operação indicada pela API Java.

```java
int[] valores = {1, 2, 3};
var resultado = Arrays.deepToString(new Object[]{valores});
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Arrays. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
