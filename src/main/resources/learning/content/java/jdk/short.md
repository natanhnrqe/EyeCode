---
id: java/jdk/short
title: Short
concept: short-wrapper
level: beginner
duration: 3
category: API JAVA
officialDocs:
  label: Short
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Short.html
related:
  - java/jdk/number
  - java/jdk/byte
  - java/jdk/integer
  - java/basics/primitive-types
---
Short é o wrapper de short, um inteiro com sinal de 16 bits.

```java
Short valor = Short.valueOf("120");
short primitivo = valor;

int ordem = Short.compare((short) 2, (short) 5);
System.out.println(Short.MIN_VALUE);
System.out.println(primitivo);
```

Short é útil em APIs que trabalham com objetos ou precisam representar null. Não o escolha apenas para economizar memória sem uma necessidade clara.
