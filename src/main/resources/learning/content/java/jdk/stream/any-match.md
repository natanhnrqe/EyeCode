---
id: java/jdk/stream/any-match
title: Stream.anyMatch()
concept: stream-any-match
kind: method
sourceMember: anyMatch
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/stream
related:
  - java/jdk/stream
---
## O que ele faz?

Usa anyMatch() em Stream para realizar a operação indicada pela API Java.

```java
Stream<String> valores = List.of("Ada", "Lin").stream();
var resultado = valores.anyMatch(texto -> texto.length() > 2);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Stream. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
