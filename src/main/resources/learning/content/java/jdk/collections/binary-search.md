---
id: java/jdk/collections/binary-search
title: Collections.binarySearch()
concept: collections-binary-search
kind: method
sourceMember: binarySearch
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collections
related:
  - java/jdk/collections
---
## O que ele faz?

Usa binarySearch() em Collections para realizar a operação indicada pela API Java.

```java
List<String> nomes = new ArrayList<>(List.of("Ada", "Lin"));
var resultado = Collections.binarySearch(nomes, "Ada");
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collections. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
