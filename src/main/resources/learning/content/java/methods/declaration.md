---
id: java/methods/declaration
title: declaração de método
concept: method-declaration
level: beginner
duration: 3
category: MÉTODOS
depth: full
related:
  - java/methods/parameters
  - java/syntax/control-flow/return
  - java/syntax/types/void
  - java/syntax/modifiers/static
  - java/methods/overloading
  - java/methods/recursion
---
Um método é um bloco nomeado que pode receber dados, executar uma tarefa e devolver um resultado. Sua declaração reúne modificadores, tipo de retorno, nome, parâmetros e corpo.

```java
public int somar(int a, int b) {
    return a + b;
}
```

Nesta declaração, `public` é um modificador de visibilidade, `int` é o tipo de retorno, `somar` é o nome e `(int a, int b)` é a lista de parâmetros. O corpo fica entre chaves. A chamada fornece argumentos:

```java
int total = somar(2, 3);
```

Métodos de instância dependem de um objeto; métodos `static` pertencem à classe. Parâmetros, `return`, `void` e `static` têm explicações próprias nos cards relacionados.
