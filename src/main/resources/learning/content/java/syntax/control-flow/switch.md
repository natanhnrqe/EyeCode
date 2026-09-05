---
id: java/syntax/control-flow/switch
title: switch
concept: switch
level: beginner
duration: 3
category: FLUXO DE CONTROLE
depth: full
related:
  - java/syntax/control-flow/case
  - java/syntax/control-flow/default
  - java/syntax/control-flow/break
  - java/syntax/control-flow/yield
  - java/syntax/control-flow/if
---
switch escolhe um ramo com base em um valor. No formato tradicional, casos com dois-pontos podem continuar até encontrar break.

~~~java
switch (dia) {
    case 1:
        nome = "domingo";
        break;
    default:
        nome = "outro";
}
~~~

Uma expressão switch produz um valor e pode usar setas, múltiplos rótulos e yield:

~~~java
int dias = switch (mes) {
    case 2 -> 28;
    case 4, 6, 9, 11 -> 30;
    default -> 31;
};
~~~

A sintaxe com setas não tem fall-through implícito. Pattern matching fica fora deste card.
