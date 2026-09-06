---
id: java/jdk/object/finalize
title: Object.finalize()
concept: object-finalize
kind: method
sourceMember: finalize
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/object
related:
  - java/jdk/object
---
## O que ele faz?

Usa finalize() em Object para realizar a operação indicada pela API Java.

```java
Object recurso = new Object();
System.out.println(recurso);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Object. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
