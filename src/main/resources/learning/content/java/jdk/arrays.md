---
id: java/jdk/arrays
title: Arrays
concept: arrays-class
level: beginner
duration: 5
category: API JAVA
officialDocs:
  label: Arrays
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/Arrays.html
related:
  - java/arrays/arrays
  - java/arrays/array-index
  - java/arrays/multidimensional
---
java.util.Arrays reúne operações utilitárias para arrays. Ela não é a própria array.

```java
import java.util.Arrays;

int[] numeros = {4, 1, 3, 2};
Arrays.sort(numeros);

System.out.println(Arrays.toString(numeros));
System.out.println(Arrays.binarySearch(numeros, 3));
```

Também há equals, fill e copyOf. Use array.length para arrays, String.length() para strings e size() para coleções.

```java
int[] original = {1, 2, 3};
int[] copia = Arrays.copyOf(original, 5);
Arrays.fill(copia, 3, copia.length, 0);

System.out.println(Arrays.equals(original, new int[]{1, 2, 3}));
```
