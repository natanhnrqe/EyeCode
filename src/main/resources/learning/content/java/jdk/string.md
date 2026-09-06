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
  - java/jdk/char-sequence
  - java/jdk/string-builder
members:
  - length(): java/jdk/string/length
  - isBlank(): java/jdk/string/is-blank
  - substring(): java/jdk/string/substring
  - contains(): java/jdk/string/contains
  - replace(): java/jdk/string/replace
  - split(): java/jdk/string/split
- isEmpty(): java/jdk/string/is-empty
- charAt(): java/jdk/string/char-at
- startsWith(): java/jdk/string/starts-with
- endsWith(): java/jdk/string/ends-with
- indexOf(): java/jdk/string/index-of
- lastIndexOf(): java/jdk/string/last-index-of
- equals(): java/jdk/string/equals
- equalsIgnoreCase(): java/jdk/string/equals-ignore-case
- compareTo(): java/jdk/string/compare-to
- replaceAll(): java/jdk/string/replace-all
- trim(): java/jdk/string/trim
- strip(): java/jdk/string/strip
- toLowerCase(): java/jdk/string/to-lower-case
- toUpperCase(): java/jdk/string/to-upper-case
- formatted(): java/jdk/string/formatted
- join(): java/jdk/string/join
- valueOf(): java/jdk/string/value-of
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
