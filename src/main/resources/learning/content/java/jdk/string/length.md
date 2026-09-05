---
id: java/jdk/string/length
title: String.length()
concept: string-length
kind: method
sourceMember: length
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

`length()` retorna a quantidade de unidades de código UTF-16 de uma string.

```java
int size = "EyeCode".length();
```

O retorno é um `int`. Para caracteres Unicode suplementares, lembre que a contagem é de unidades UTF-16, não de grafemas percebidos pelo usuário.
