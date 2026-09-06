---
id: java/jdk/comparator/comparing
title: Comparator.comparing()
concept: comparator-comparing
kind: method
sourceMember: comparing
level: beginner
duration: 2
category: API JAVA
parent: java/jdk/comparator
related:
  - java/jdk/comparator
---
## O que ele faz?

Cria um comparador extraindo uma chave de cada elemento.

```java
nomes.sort(Comparator.comparing(String::length));
```

## Comportamento

O resultado e os efeitos dependem dos argumentos e do contrato de Comparator. Verifique o retorno antes de continuar quando a operação puder indicar ausência, falha ou mudança de estado.
