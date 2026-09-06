---
id: java/jdk/collections/unmodifiable-map
title: Collections.unmodifiableMap()
concept: collections-unmodifiable-map
kind: method
sourceMember: unmodifiableMap
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/collections
related:
  - java/jdk/collections
---
## O que ele faz?

Usa unmodifiableMap() em Collections para realizar a operação indicada pela API Java.

```java
List<String> nomes = new ArrayList<>(List.of("Ada", "Lin"));
var resultado = Collections.unmodifiableMap(Map.of("Ada", 1));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Collections. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
