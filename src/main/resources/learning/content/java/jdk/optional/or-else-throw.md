---
id: java/jdk/optional/or-else-throw
title: Optional.orElseThrow()
concept: optional-or-else-throw
kind: method
sourceMember: orElseThrow
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/optional
related:
  - java/jdk/optional
---
## O que ele faz?

Usa orElseThrow() em Optional para realizar a operação indicada pela API Java.

```java
Optional<String> valor = Optional.of("Ada");
var resultado = valor.orElseThrow();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Optional. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
