---
id: java/jdk/arrays/deep-equals
title: Arrays.deepEquals()
concept: arrays-deep-equals
kind: method
sourceMember: deepEquals
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/arrays
related:
  - java/jdk/arrays
---
## O que ele faz?

Usa deepEquals() em Arrays para realizar a operação indicada pela API Java.

```java
int[] valores = {1, 2, 3};
var resultado = Arrays.deepEquals(new Object[]{valores}, new Object[]{valores});
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Arrays. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
