---
id: java/jdk/collections/unmodifiable-list
title: Collections.unmodifiableList()
concept: collections-unmodifiable-list
kind: method
sourceMember: unmodifiableList
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collections
related:
  - java/jdk/collections
---
## O que ele faz?

Usa unmodifiableList() em Collections para realizar a operação indicada pela API Java.

```java
List<String> nomes = new ArrayList<>(List.of("Ada", "Lin"));
var resultado = Collections.unmodifiableList(nomes);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collections. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
