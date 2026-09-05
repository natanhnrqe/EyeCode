---
id: java/syntax/types/void
title: void
concept: void
level: beginner
duration: 2
category: SISTEMA DE TIPOS
depth: quick
related:
  - java/syntax/control-flow/return
  - java/methods/declaration
  - java/syntax/modifiers/static
---
void indica que um método não devolve um valor.

~~~java
void imprimir(String texto) {
    System.out.println(texto);
}
~~~

Um método void ainda pode usar return; para terminar antes do fim, mas não pode usar return valor.
