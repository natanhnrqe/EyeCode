---
id: java/syntax/control-flow/case
title: case
concept: case
level: beginner
duration: 2
category: FLUXO DE CONTROLE
depth: quick
related:
  - java/syntax/control-flow/switch
  - java/syntax/control-flow/default
  - java/syntax/control-flow/break
  - java/syntax/control-flow/yield
---
case identifica um valor possível dentro de um switch. É possível agrupar rótulos em uma expressão moderna.

~~~java
String nome = switch (mes) {
    case 1, 2, 3 -> "primeiro trimestre";
    default -> "outro período";
};
~~~

Em um switch tradicional, casos com dois-pontos podem cair no caso seguinte; use break para interromper. Em uma expressão, yield produz o valor do caso.
