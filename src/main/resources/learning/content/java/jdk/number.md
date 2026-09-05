---
id: java/jdk/number
title: Number
concept: number
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Number
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Number.html
related:
  - java/jdk/integer
  - java/jdk/long
  - java/jdk/double
  - java/basics/primitive-types
---
Number é uma superclasse abstrata para diferentes wrappers numéricos. A mesma referência pode apontar para um Integer ou um Double.

```java
Number inteiro = 10;
Number decimal = 10.5;

System.out.println(inteiro.intValue());
System.out.println(decimal.doubleValue());
```

Os métodos intValue(), longValue(), floatValue() e doubleValue() convertem o valor. A conversão pode perder precisão ou faixa.

```java
Number valor = 10.75;
int truncado = valor.intValue();

System.out.println(truncado); // 10
```
