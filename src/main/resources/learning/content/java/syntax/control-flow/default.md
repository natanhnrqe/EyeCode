---
id: java/syntax/control-flow/default
title: default
concept: default
level: beginner
duration: 2
category: FLUXO DE CONTROLE
depth: quick
related:
  - java/syntax/control-flow/switch
  - java/syntax/control-flow/case
  - java/syntax/control-flow/yield
---
default é o ramo usado quando nenhum case corresponde.

~~~java
int dias = switch (mes) {
    case 2 -> 28;
    default -> 31;
};
~~~

Em uma expressão switch, o ramo deve produzir um valor diretamente ou com yield dentro de um bloco.
