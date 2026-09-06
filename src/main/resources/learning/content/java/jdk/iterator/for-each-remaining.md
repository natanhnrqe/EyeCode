---
id: java/jdk/iterator/for-each-remaining
title: Iterator.forEachRemaining()
concept: iterator-for-each-remaining
kind: method
sourceMember: forEachRemaining
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/iterator
related:
  - java/jdk/iterator
---
## O que ele faz?

Usa forEachRemaining() em Iterator para realizar a operação indicada pela API Java.

```java
Iterator<String> valores = List.of("Ada").iterator();
valores.forEachRemaining(System.out::println);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Iterator. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
