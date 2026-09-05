---
id: java/jdk/double
title: Double
concept: double-wrapper
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Double
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Double.html
related:
  - java/jdk/number
  - java/jdk/float
  - java/jdk/integer
  - java/basics/primitive-types
---
Double é o wrapper de double, o ponto flutuante de precisão dupla usado em cálculos gerais.

```java
double resultado = 0.1 + 0.2;
Double valor = Double.valueOf("3.14");

System.out.println(resultado);
System.out.println(Double.isNaN(valor));
System.out.println(Double.isInfinite(valor));
```

Valores de ponto flutuante são aproximados. Para aritmética monetária exata, avalie BigDecimal em java.math.
