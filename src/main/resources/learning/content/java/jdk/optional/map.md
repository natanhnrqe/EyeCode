---
id: java/jdk/optional/map
title: Optional.map()
concept: optional-map
kind: method
sourceMember: map
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/optional
related:
  - java/jdk/optional
---
## O que ele faz?

Transforma o valor presente sem abrir mão da representação de ausência.

```java
Optional<Integer> tamanho = opcional.map(String::length);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Optional. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
