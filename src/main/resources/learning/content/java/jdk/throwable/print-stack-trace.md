---
id: java/jdk/throwable/print-stack-trace
title: Throwable.printStackTrace()
concept: throwable-print-stack-trace
kind: method
sourceMember: printStackTrace
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/throwable
related:
  - java/jdk/throwable
---
## O que ele faz?

Usa printStackTrace() em Throwable para realizar a operação indicada pela API Java.

```java
Throwable erro = new RuntimeException("falha");
erro.printStackTrace();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Throwable. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
