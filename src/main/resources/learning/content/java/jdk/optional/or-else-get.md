---
id: java/jdk/optional/or-else-get
title: Optional.orElseGet()
concept: optional-or-else-get
kind: method
sourceMember: orElseGet
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/optional
related:
  - java/jdk/optional
---
## O que ele faz?

Produz um valor alternativo sob demanda quando o Optional está vazio.

```java
String nome = opcional.orElseGet(() -> "desconhecido");
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Optional. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
