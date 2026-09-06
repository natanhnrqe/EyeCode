---
id: java/jdk/comparator/reverse-order
title: Comparator.reverseOrder()
concept: comparator-reverse-order
kind: method
sourceMember: reverseOrder
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/comparator
related:
  - java/jdk/comparator
---
## O que ele faz?

Usa reverseOrder() em Comparator para realizar a operação indicada pela API Java.

```java
Comparator<String> comparador = Comparator.naturalOrder();
var resultado = comparador.reverseOrder();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Comparator. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
