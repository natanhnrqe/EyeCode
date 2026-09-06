---
id: java/jdk/long
title: Long
concept: long-wrapper
level: beginner
duration: 4
members:
- parseLong(): java/jdk/long/parse-long
- valueOf(): java/jdk/long/value-of
- compare(): java/jdk/long/compare
- compareTo(): java/jdk/long/compare-to
- longValue(): java/jdk/long/long-value
category: API JAVA
officialDocs:
  label: Long
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Long.html
related:
  - java/jdk/number
  - java/jdk/integer
  - java/jdk/double
  - java/basics/primitive-types
---
Long é o wrapper de long, usado para inteiros além da faixa de int. Literais long normalmente usam L.

```java
long populacao = 8_000_000_000L;
Long objeto = Long.valueOf(populacao);

long valor = Long.parseLong("42");
int ordem = Long.compare(10L, 20L);
System.out.println(Long.MAX_VALUE);
```

parseLong retorna long e valueOf retorna Long. Long.MIN_VALUE e Long.MAX_VALUE representam os limites do tipo.
