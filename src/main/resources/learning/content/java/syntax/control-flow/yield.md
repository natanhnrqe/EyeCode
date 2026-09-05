---
id: java/syntax/control-flow/yield
title: yield
concept: yield
level: beginner
duration: 2
category: FLUXO DE CONTROLE
depth: quick
related:
  - java/syntax/control-flow/switch
  - java/syntax/control-flow/case
  - java/syntax/control-flow/default
  - java/syntax/control-flow/return
---
`yield` produz um valor dentro de um bloco de uma expressão `switch`. Ele não encerra o método como `return`; entrega o resultado daquela expressão `switch`.

```java
int dias = switch (mes) {
    case 2 -> 28;
    case 4, 6, 9, 11 -> 30;
    default -> {
        int valor = 31;
        yield valor;
    }
};
```

Use `return` para sair do método e `yield` para fornecer o valor de um bloco da expressão `switch`.


Use yield apenas para fornecer o valor de um bloco da expressão switch; ele não retorna do método.