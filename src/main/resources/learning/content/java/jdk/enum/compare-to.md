---
id: java/jdk/enum/compare-to
title: Enum.compareTo()
concept: enum-compare-to
kind: method
sourceMember: compareTo
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/enum
related:
  - java/jdk/enum
---
## O que ele faz?

Usa compareTo() em Enum para realizar a operação indicada pela API Java.

```java
enum Status { OK }
Status valor = Status.OK;
var resultado = valor.compareTo(Status.OK);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Enum. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
