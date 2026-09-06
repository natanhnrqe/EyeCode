---
id: java/jdk/throwable/get-stack-trace
title: Throwable.getStackTrace()
concept: throwable-get-stack-trace
kind: method
sourceMember: getStackTrace
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/throwable
related:
  - java/jdk/throwable
---
## O que ele faz?

Usa getStackTrace() em Throwable para realizar a operação indicada pela API Java.

```java
Throwable erro = new RuntimeException("falha");
var resultado = erro.getStackTrace();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Throwable. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
