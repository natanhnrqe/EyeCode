---
id: java/jdk/map/entry-set
title: Map.entrySet()
concept: map-entry-set
kind: method
sourceMember: entrySet
sourceSignature: ()
level: beginner
duration: 2
category: COLEÇÕES
parent: java/jdk/map
related:
  - java/jdk/map
---
## O que ele faz?

Expõe a visão das entradas, cada uma com chave e valor.

```java
for (Map.Entry<String, Integer> entrada : mapa.entrySet()) {
    System.out.println(entrada.getKey());
}
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Map. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
