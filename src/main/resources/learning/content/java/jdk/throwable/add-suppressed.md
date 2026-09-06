---
id: java/jdk/throwable/add-suppressed
title: Throwable.addSuppressed()
concept: throwable-add-suppressed
kind: method
sourceMember: addSuppressed
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/throwable
related:
  - java/jdk/throwable
---
## O que ele faz?

Usa addSuppressed() em Throwable para realizar a operação indicada pela API Java.

```java
Throwable erro = new RuntimeException("falha");
erro.addSuppressed(new Exception("causa"));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Throwable. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
