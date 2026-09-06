---
id: java/jdk/comparator/natural-order
title: Comparator.naturalOrder()
concept: comparator-natural-order
kind: method
sourceMember: naturalOrder
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/comparator
related:
  - java/jdk/comparator
---
## O que ele faz?

Usa naturalOrder() em Comparator para realizar a operação indicada pela API Java.

```java
Comparator<String> comparador = Comparator.naturalOrder();
var resultado = comparador.naturalOrder();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Comparator. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
