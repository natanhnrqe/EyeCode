---
id: java/jdk/boolean
title: Boolean
concept: boolean-wrapper
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: Boolean
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Boolean.html
related:
  - java/basics/primitive-types
  - java/jdk/character
  - java/jdk/object
---
Boolean é o wrapper de boolean. O primitivo guarda true ou false; o wrapper é uma referência e pode ser null.

```java
boolean primitivo = true;
Boolean wrapper = primitivo;

boolean lido = Boolean.parseBoolean("true");
Boolean objeto = Boolean.valueOf("false");
System.out.println(Boolean.compare(lido, objeto));
```

parseBoolean retorna boolean e valueOf retorna Boolean.

```java
Boolean ativo = null;

// O unboxing implícito aqui pode lançar NullPointerException.
if (ativo) {
    System.out.println("Ativo");
}
```
