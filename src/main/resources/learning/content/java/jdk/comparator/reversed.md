---
id: java/jdk/comparator/reversed
title: Comparator.reversed()
concept: comparator-reversed
kind: method
sourceMember: reversed
sourceSignature: ()
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/comparator
related:
  - java/jdk/comparator
---
## O que ele faz?

Usa reversed() em Comparator para realizar a operação indicada pela API Java.

```java
Comparator<String> comparador = Comparator.naturalOrder();
var resultado = comparador.reversed();
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Comparator. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
