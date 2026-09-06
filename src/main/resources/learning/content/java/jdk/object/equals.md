---
id: java/jdk/object/equals
title: Object.equals()
concept: object-equals
kind: method
sourceMember: equals
sourceSignature: (Object)
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/object
related:
  - java/jdk/object
---
## O que ele faz?

Usa equals() em Object para realizar a operação indicada pela API Java.

```java
Object primeiro = new Object();
boolean igual = primeiro.equals(primeiro);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Object. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
