---
id: java/syntax/control-flow/if
title: if
concept: if
level: beginner
duration: 2
category: FLUXO DE CONTROLE
depth: full
related:
  - java/syntax/control-flow/else
  - java/syntax/control-flow/switch
  - java/basics/primitive-types
  - java/basics/operators
  - java/syntax/control-flow/return
---
if executa um bloco quando uma condição booleana é verdadeira. Use chaves para deixar cada ramo explícito.

~~~java
if (idade >= 18) {
    permitirEntrada();
} else if (idade >= 16) {
    exigirAcompanhante();
} else {
    recusarEntrada();
}
~~~

A condição pode combinar expressões com operadores lógicos. && usa curto-circuito: a segunda parte só é avaliada quando necessária. return pode encerrar o método antecipadamente.
