---
id: java/jdk/throwable/get-suppressed
title: Throwable.getSuppressed()
concept: throwable-get-suppressed
kind: method
sourceMember: getSuppressed
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/throwable
related:
  - java/jdk/throwable
---
## O que ele faz?

Usa getSuppressed() em Throwable para realizar a operação indicada pela API Java.

```java
Throwable erro = new RuntimeException("falha");
var resultado = erro.getSuppressed();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Throwable. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
