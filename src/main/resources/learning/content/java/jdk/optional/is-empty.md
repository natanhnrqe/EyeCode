---
id: java/jdk/optional/is-empty
title: Optional.isEmpty()
concept: optional-is-empty
kind: method
sourceMember: isEmpty
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/optional
related:
  - java/jdk/optional
---
## O que ele faz?

Usa isEmpty() em Optional para realizar a operação indicada pela API Java.

```java
Optional<String> valor = Optional.of("Ada");
var resultado = valor.isEmpty();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Optional. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
