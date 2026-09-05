---
id: java/generics/wildcards
title: Wildcards
concept: wildcards
level: beginner
duration: 5
category: FUNDAMENTOS
related:
  - java/generics/generics
  - java/generics/type-bounds
  - java/generics/type-erasure
---
O wildcard ? representa um tipo desconhecido em uma parametrização.

```java
static void imprimir(java.util.List<?> valores) {
    for (Object valor : valores) {
        System.out.println(valor);
    }
}
```

Com ? extends Number, a lista é produtora de valores Number. Com ? super Integer, ela pode consumir Integer.

```java
java.util.List<? extends Number> produtores = java.util.List.of(1, 2.5);
java.util.List<? super Integer> consumidores = new java.util.ArrayList<Number>();
consumidores.add(10);
```

Uma List<Integer> não pode ser usada como List<Number>. A intuição PECS significa Producer Extends e Consumer Super.
