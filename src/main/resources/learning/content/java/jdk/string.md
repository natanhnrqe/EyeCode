---
id: java/jdk/string
title: String
concept: string
level: beginner
duration: 4
category: API JAVA
officialDocs:
  label: String
  url: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/String.html
related:
  - java/jdk/object
members:
  - length(): java/jdk/string/length
  - isBlank(): java/jdk/string/is-blank
  - substring(): java/jdk/string/substring
  - contains(): java/jdk/string/contains
  - replace(): java/jdk/string/replace
  - split(): java/jdk/string/split
---
## O que é isso?

`String` representa texto. Strings são imutáveis, portanto cada operação produz um novo valor.

```java
String greeting = "Hello";
String loud = greeting.toUpperCase();
```

Use `equals` para comparar texto, e não `==`.

## Métodos comuns

Escolha um método abaixo para explorar operações frequentes de `String`.
