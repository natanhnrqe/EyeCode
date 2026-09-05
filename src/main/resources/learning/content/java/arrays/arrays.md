---
id: java/arrays/arrays
title: Arrays / Vetores
concept: arrays
level: beginner
duration: 5
category: FUNDAMENTOS
related:
  - java/arrays/array-index
  - java/arrays/multidimensional
  - java/jdk/arrays
  - java/syntax/control-flow/for
---
Arrays armazenam uma quantidade fixa de elementos compatíveis. O tamanho é definido na criação e não muda depois.

```java
int[] numeros = {10, 20, 30};

System.out.println(numeros[0]);
System.out.println(numeros.length);

numeros[1] = 99;
```

A indexação começa em zero. O campo length informa a quantidade de posições.

```java
String[] nomes = new String[3];
nomes[0] = "Ana";
nomes[1] = "Bruno";

for (String nome : nomes) {
    System.out.println(nome);
}
```

Uma array é um objeto e suas posições começam com valores padrão: zero para números, false para boolean e null para referências.
