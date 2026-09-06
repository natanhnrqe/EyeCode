---
id: java/jdk/optional/filter
title: Optional.filter()
concept: optional-filter
kind: method
sourceMember: filter
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/optional
related:
  - java/jdk/optional
---
## O que ele faz?

Usa filter() em Optional para realizar a operação indicada pela API Java.

```java
Optional<String> valor = Optional.of("Ada");
var resultado = valor.filter(texto -> texto.length() > 2);
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Optional. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
