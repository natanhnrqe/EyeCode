---
id: java/jdk/string/substring
title: String.substring()
concept: string-substring
kind: method
sourceMember: substring
sourceSignature: (int, int)
level: beginner
duration: 2
category: API JAVA
depth: quick
related:
  - java/jdk/string
parent: java/jdk/string
---
## O que ele faz?

`substring(begin, end)` retorna os caracteres de `begin`, inclusivo, até `end`, exclusivo.

```java
String code = "EyeCode".substring(0, 3);
```

Os índices devem estar dentro da string e em ordem crescente.

## Assinaturas comuns

Use `substring(beginIndex)` para obter tudo de um índice até o final, ou `substring(beginIndex, endIndex)` para definir um limite superior exclusivo.

O valor retornado é uma nova `String`. O texto original permanece inalterado.
