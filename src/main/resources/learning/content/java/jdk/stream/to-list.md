---
id: java/jdk/stream/to-list
title: Stream.toList()
concept: stream-to-list
kind: method
sourceMember: toList
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/stream
related:
  - java/jdk/stream
---
## O que ele faz?

Materializa os elementos do stream em uma lista não modificável.

```java
List<String> resultado = nomes.stream().map(String::trim).toList();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Stream. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
