---
id: java/jdk/stream/limit
title: Stream.limit()
concept: stream-limit
kind: method
sourceMember: limit
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/stream
related:
  - java/jdk/stream
---
## O que ele faz?

Usa limit() em Stream para realizar a operação indicada pela API Java.

```java
Stream<String> valores = List.of("Ada", "Lin").stream();
var resultado = valores.limit(1);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Stream. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
