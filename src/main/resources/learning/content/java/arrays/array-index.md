---
id: java/arrays/array-index
title: Índices de array
concept: array-index
level: beginner
duration: 3
category: FUNDAMENTOS
related:
  - java/arrays/arrays
  - java/arrays/multidimensional
  - java/jdk/arrays
---
Cada posição de uma array é acessada por um índice. O primeiro índice é zero e o último é length - 1.

```java
int[] valores = {10, 20, 30};

int primeiro = valores[0];
int ultimo = valores[valores.length - 1];

System.out.println(primeiro);
System.out.println(ultimo);
```

O índice válido obedece a 0 <= índice < array.length. Um índice fora desse intervalo causa ArrayIndexOutOfBoundsException.
