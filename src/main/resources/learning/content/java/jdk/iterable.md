---
id: java/jdk/iterable
title: Iterable
concept: iterable
level: beginner
duration: 3
category: API JAVA
officialDocs:
  label: Iterable
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/Iterable.html
related:
  - java/basics/reference-types
  - java/jdk/string
  - java/jdk/char-sequence
  - java/jdk/list
  - java/generics/generics
---
Iterable representa algo que pode fornecer um Iterator e participar do enhanced for-loop.

```java
Iterable<String> nomes = java.util.List.of("Ana", "Bruno", "Carla");

for (String nome : nomes) {
    System.out.println(nome);
}
```

O loop funciona porque o objeto fornece iterator(). Coleções são os exemplos mais comuns, mas qualquer tipo pode implementar Iterable.
