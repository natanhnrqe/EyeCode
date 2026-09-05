---
id: java/jdk/string/is-blank
title: String.isBlank()
concept: string-is-blank
kind: method
sourceMember: isBlank
sourceSignature: ()
level: beginner
duration: 1
category: API JAVA
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---
## O que ele faz?

`isBlank()` é verdadeiro quando a string está vazia ou contém somente espaços em branco.

```java
boolean empty = "   ".isBlank();
```

Diferente de `isEmpty()`, este método considera texto formado apenas por espaços como vazio.
