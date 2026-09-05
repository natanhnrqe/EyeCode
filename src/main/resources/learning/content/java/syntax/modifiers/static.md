---
id: java/syntax/modifiers/static
title: static
concept: static
level: beginner
duration: 2
category: MODIFICADORES
depth: quick
related:
  - java/syntax/modifiers/final
  - java/types/class
  - java/methods/declaration
  - java/basics/variables
---
static indica que um membro pertence à classe, e não a uma instância específica. Isso vale para métodos e campos estáticos.

~~~java
static int maior(int a, int b) {
    return Math.max(a, b);
}

int resultado = Exemplo.maior(2, 3);
~~~

Um método estático é chamado usando a classe e não precisa de um objeto. static não significa global.
