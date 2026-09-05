---
id: java/jdk/float
title: Float
concept: float-wrapper
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Float
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Float.html
related:
  - java/jdk/number
  - java/jdk/double
  - java/basics/primitive-types
---
Float é o wrapper de float, um ponto flutuante de precisão simples.

```java
float taxa = 1.5f;
Float valor = Float.valueOf("2.75");

System.out.println(Float.isNaN(valor));
System.out.println(Float.MAX_VALUE);
```

Float.parseFloat retorna float e valueOf retorna Float. O tipo também representa NaN, POSITIVE_INFINITY e NEGATIVE_INFINITY conforme IEEE 754.
