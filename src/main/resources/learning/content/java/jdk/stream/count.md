---
id: java/jdk/stream/count
title: Stream.count()
concept: stream-count
kind: method
sourceMember: count
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/stream
related:
  - java/jdk/stream
---
## O que ele faz?

Usa count() em Stream para realizar a operação indicada pela API Java.

```java
Stream<String> valores = List.of("Ada", "Lin").stream();
var resultado = valores.count();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Stream. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
