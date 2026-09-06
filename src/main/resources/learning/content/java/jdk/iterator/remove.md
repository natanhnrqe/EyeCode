---
id: java/jdk/iterator/remove
title: Iterator.remove()
concept: iterator-remove
kind: method
sourceMember: remove
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/iterator
related:
  - java/jdk/iterator
---
## O que ele faz?

Usa remove() em Iterator para realizar a operação indicada pela API Java.

```java
Iterator<String> valores = List.of("Ada").iterator();
valores.remove();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Iterator. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
