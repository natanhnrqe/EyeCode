---
id: java/jdk/collections/unmodifiable-set
title: Collections.unmodifiableSet()
concept: collections-unmodifiable-set
kind: method
sourceMember: unmodifiableSet
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collections
related:
  - java/jdk/collections
---
## O que ele faz?

Usa unmodifiableSet() em Collections para realizar a operação indicada pela API Java.

```java
List<String> nomes = new ArrayList<>(List.of("Ada", "Lin"));
var resultado = Collections.unmodifiableSet(Set.of("Ada"));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collections. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
