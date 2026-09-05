---
id: java/syntax/control-flow/for
title: for
concept: for
level: beginner
duration: 3
category: FLUXO DE CONTROLE
depth: full
related:
  - java/syntax/control-flow/while
  - java/syntax/control-flow/continue
  - java/syntax/control-flow/break
  - java/basics/variables
  - java/basics/operators
---
for clássico reúne inicialização, condição e atualização.

~~~java
for (int i = 0; i < 10; i++) {
    processar(i);
}
~~~

Use o for aprimorado para percorrer os elementos de um array ou coleção quando não precisa controlar o índice:

~~~java
for (String nome : nomes) {
    System.out.println(nome);
}
~~~

Use break para sair e continue para iniciar a próxima iteração.
