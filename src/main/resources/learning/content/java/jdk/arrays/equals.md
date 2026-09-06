---
id: java/jdk/arrays/equals
title: Arrays.equals()
concept: arrays-equals
kind: method
sourceMember: equals
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/arrays
related:
  - java/jdk/arrays
---
## O que ele faz?

Usa equals() em Arrays para realizar a operação indicada pela API Java.

```java
int[] valores = {1, 2, 3};
var resultado = Arrays.equals(valores, new int[]{1, 2});
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Arrays. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
