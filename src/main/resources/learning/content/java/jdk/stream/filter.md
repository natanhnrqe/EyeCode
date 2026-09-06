---
id: java/jdk/stream/filter
title: Stream.filter()
concept: stream-filter
kind: method
sourceMember: filter
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/stream
related:
  - java/jdk/stream
---
## O que ele faz?

Mantém no pipeline apenas os elementos aceitos por um predicado.

```java
List<String> longos = nomes.stream().filter(nome -> nome.length() > 3).toList();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Stream. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
