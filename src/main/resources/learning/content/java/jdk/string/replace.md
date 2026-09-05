---
id: java/jdk/string/replace
title: String.replace()
concept: string-replace
kind: method
sourceMember: replace
sourceSignature: (CharSequence, CharSequence)
level: beginner
duration: 1
category: API JAVA
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---
## O que ele faz?

`replace()` retorna uma nova string com caracteres ou sequências correspondentes substituídos.

```java
String normalized = "Eye Code".replace(" ", "");
```

A `String` original não é alterada.

Para substituições com expressão regular, use `replaceAll()`. Para a primeira correspondência literal, compare o comportamento de `replaceFirst()`.
