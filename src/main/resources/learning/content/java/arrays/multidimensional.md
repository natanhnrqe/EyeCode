---
id: java/arrays/multidimensional
title: Arrays multidimensionais
concept: multidimensional-arrays
level: beginner
duration: 4
category: FUNDAMENTOS
related:
  - java/arrays/arrays
  - java/arrays/array-index
  - java/jdk/arrays
---
Em Java, uma array multidimensional é uma array cujos elementos também são arrays.

```java
int[][] matriz = new int[2][3];
matriz[0][1] = 7;

int[][] dados = {
    {1, 2},
    {3, 4, 5}
};

System.out.println(dados[1][2]);
```

As linhas podem ter tamanhos diferentes. Portanto, int[][] não garante uma matriz retangular; cada linha possui seu próprio length.
