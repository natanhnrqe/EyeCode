---
id: java/generics/type-bounds
title: Limites de tipo
concept: type-bounds
level: beginner
duration: 3
category: FUNDAMENTOS
related:
  - java/generics/generics
  - java/generics/wildcards
  - java/jdk/number
---
Um limite restringe quais tipos podem ser usados em um parâmetro genérico. Neste contexto, extends expressa um limite superior.

```java
static <T extends Number> double dobro(T valor) {
    return valor.doubleValue() * 2;
}

System.out.println(dobro(10));
System.out.println(dobro(2.5));
```

O método pode chamar operações garantidas por Number. Esse extends de generics não é uma declaração de herança da classe do método.
