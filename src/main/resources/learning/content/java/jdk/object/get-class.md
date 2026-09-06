---
id: java/jdk/object/get-class
title: Object.getClass()
concept: object-get-class
kind: method
sourceMember: getClass
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/object
related:
  - java/jdk/object
---
## O que ele faz?

Usa getClass() em Object para realizar a operação indicada pela API Java.

```java
Object valor = "EyeCode";
Class<?> tipo = valor.getClass();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Object. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
