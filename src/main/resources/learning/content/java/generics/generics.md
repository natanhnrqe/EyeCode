---
id: java/generics/generics
title: Generics
concept: generics
level: beginner
duration: 5
category: FUNDAMENTOS
related:
  - java/generics/generic-classes
  - java/generics/generic-methods
  - java/generics/type-bounds
  - java/generics/wildcards
  - java/generics/type-erasure
---
Generics permitem escrever APIs reutilizáveis mantendo a verificação de tipos em tempo de compilação.

```java
java.util.List<String> nomes = new java.util.ArrayList<>();
nomes.add("Ana");
String primeiro = nomes.get(0);
```

O compilador impede inserir um tipo incompatível e reduz casts desnecessários. List<String> e List<Integer> são parametrizações diferentes; List<Integer> não é subtipo de List<Number>.
