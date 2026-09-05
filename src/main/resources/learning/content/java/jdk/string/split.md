---
id: java/jdk/string/split
title: String.split()
concept: string-split
kind: method
sourceMember: split
sourceSignature: (String)
level: beginner
duration: 2
category: API JAVA
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---
## O que ele faz?

`split()` divide uma string ao redor de um delimitador que é uma expressão regular.

```java
String[] words = "learn java".split(" ");
```

Lembre que o delimitador é uma expressão regular, e não necessariamente um caractere literal.

O resultado é um novo array. Use a sobrecarga com limite quando os campos vazios finais ou o número de divisões forem importantes.
